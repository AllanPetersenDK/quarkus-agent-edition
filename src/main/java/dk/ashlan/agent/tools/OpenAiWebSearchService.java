package dk.ashlan.agent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class OpenAiWebSearchService implements WebSearchTool.WebSearchBackend {
    private static final String DEFAULT_MODEL = "gpt-4.1-mini";
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final String INCLUDE_SOURCES = "web_search_call.action.sources";

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Duration requestTimeout;
    private final ResponsesTransport transport;
    private final ObjectMapper objectMapper;

    @Inject
    public OpenAiWebSearchService(Config config) {
        this(
                config.getOptionalValue("openai.api-key", String.class).orElse(""),
                config.getOptionalValue("openai.model", String.class).orElse(DEFAULT_MODEL),
                config.getOptionalValue("openai.base-url", String.class).orElse(DEFAULT_BASE_URL),
                config.getOptionalValue("openai.timeout-seconds", Integer.class).orElse(DEFAULT_TIMEOUT_SECONDS),
                new DefaultResponsesTransport(),
                new ObjectMapper()
        );
    }

    OpenAiWebSearchService(
            String apiKey,
            String model,
            String baseUrl,
            int timeoutSeconds,
            ResponsesTransport transport,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.requestTimeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    @Override
    public WebSearchResult search(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isBlank()) {
            throw new IllegalArgumentException("Web search query must not be blank.");
        }
        if (apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured for web-search.");
        }

        try {
            String payload = buildPayload(normalizedQuery);
            OpenAiResponse response = transport.post(URI.create(baseUrl + "/responses"), apiKey, payload, requestTimeout);
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("OpenAI web-search failed with HTTP " + response.statusCode() + ": " + response.body());
            }
            return parseResponse(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("OpenAI web-search response could not be parsed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI web-search request was interrupted", exception);
        }
    }

    private String buildPayload(String query) throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", model);
        ArrayNode tools = payload.putArray("tools");
        ObjectNode tool = tools.addObject();
        tool.put("type", "web_search");
        payload.put("tool_choice", "auto");
        ArrayNode include = payload.putArray("include");
        include.add(INCLUDE_SOURCES);
        payload.put("input", query);
        return objectMapper.writeValueAsString(payload);
    }

    private WebSearchResult parseResponse(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        String summary = extractSummary(root);
        if (summary.isBlank()) {
            throw new IllegalStateException("OpenAI web-search response did not include assistant text.");
        }
        List<WebSearchSource> sources = extractSources(root);
        return new WebSearchResult(summary, sources);
    }

    private String extractSummary(JsonNode root) {
        String topLevelOutputText = root.path("output_text").asText("");
        if (!topLevelOutputText.isBlank()) {
            return normalizeWhitespace(topLevelOutputText);
        }

        JsonNode output = root.path("output");
        if (!output.isArray()) {
            return "";
        }

        for (JsonNode item : output) {
            if (!"message".equals(item.path("type").asText(""))) {
                continue;
            }
            JsonNode content = item.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                if ("output_text".equals(contentItem.path("type").asText(""))) {
                    String text = contentItem.path("text").asText("");
                    if (!text.isBlank()) {
                        return normalizeWhitespace(text);
                    }
                }
            }
        }

        return "";
    }

    private List<WebSearchSource> extractSources(JsonNode root) {
        Map<String, WebSearchSource> sources = new LinkedHashMap<>();
        JsonNode output = root.path("output");
        if (!output.isArray()) {
            return List.of();
        }

        for (JsonNode item : output) {
            if (!"message".equals(item.path("type").asText(""))) {
                continue;
            }
            JsonNode content = item.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                JsonNode annotations = contentItem.path("annotations");
                if (!annotations.isArray()) {
                    continue;
                }
                for (JsonNode annotation : annotations) {
                    if (!"url_citation".equals(annotation.path("type").asText(""))) {
                        continue;
                    }
                    String url = annotation.path("url").asText("");
                    if (url.isBlank() || sources.containsKey(url)) {
                        continue;
                    }
                    String title = annotation.path("title").asText(url);
                    sources.put(url, new WebSearchSource(title, url));
                }
            }
        }

        return List.copyOf(sources.values());
    }

    private String normalizeWhitespace(String input) {
        return input == null ? "" : input.replaceAll("\\s+", " ").trim();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl.trim();
    }

    interface ResponsesTransport {
        OpenAiResponse post(URI uri, String apiKey, String payload, Duration timeout) throws IOException, InterruptedException;
    }

    static final class DefaultResponsesTransport implements ResponsesTransport {
        private final HttpClient httpClient = HttpClient.newBuilder().build();

        @Override
        public OpenAiResponse post(URI uri, String apiKey, String payload, Duration timeout) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new OpenAiResponse(response.statusCode(), response.body());
        }
    }

    record OpenAiResponse(int statusCode, String body) {
    }

    public record WebSearchResult(String summary, List<WebSearchSource> sources) {
        String toCompactText() {
            StringBuilder builder = new StringBuilder(summary == null ? "" : summary.trim());
            if (sources != null && !sources.isEmpty()) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append("Sources: ");
                for (int index = 0; index < sources.size(); index++) {
                    WebSearchSource source = sources.get(index);
                    if (index > 0) {
                        builder.append(" | ");
                    }
                    builder.append("[").append(index + 1).append("] ");
                    if (source.title() != null && !source.title().isBlank()) {
                        builder.append(source.title()).append(" - ");
                    }
                    builder.append(source.url());
                }
            }
            return builder.toString().trim();
        }
    }

    public record WebSearchSource(String title, String url) {
    }
}
