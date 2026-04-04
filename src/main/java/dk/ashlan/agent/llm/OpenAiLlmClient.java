package dk.ashlan.agent.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.ashlan.agent.core.ExecutionContext;
import dk.ashlan.agent.tools.Tool;
import dk.ashlan.agent.tools.ToolRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import io.smallrye.faulttolerance.api.Guard;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class OpenAiLlmClient implements BaseLlmClient {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final ExecutorService TIMEOUT_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "openai-timeout");
        thread.setDaemon(true);
        return thread;
    });

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Duration requestTimeout;
    private final OpenAiTransport transport;
    private final ObjectMapper objectMapper;

    @Inject
    public OpenAiLlmClient(Config config, OpenAiTransport transport) {
        this(
                config.getOptionalValue("openai.api-key", String.class).orElse(""),
                config.getOptionalValue("openai.model", String.class).orElse("gpt-4.1-mini"),
                config.getOptionalValue("openai.base-url", String.class).orElse("https://api.openai.com/v1"),
                config.getOptionalValue("openai.timeout-seconds", Integer.class).orElse(30),
                transport,
                new ObjectMapper()
        );
    }

    OpenAiLlmClient(String apiKey, String model, String baseUrl, int timeoutSeconds, ObjectMapper objectMapper) {
        this(apiKey, model, baseUrl, timeoutSeconds, new DefaultOpenAiTransport(baseUrl, timeoutSeconds), objectMapper);
    }

    OpenAiLlmClient(String apiKey, String model, String baseUrl, int timeoutSeconds, OpenAiTransport transport, ObjectMapper objectMapper) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "gpt-4.1-mini" : model.trim();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.requestTimeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmCompletion complete(List<LlmMessage> messages, ToolRegistry toolRegistry, ExecutionContext context) {
        if (apiKey.isBlank()) {
            return LlmCompletion.answer(
                    "OpenAI client placeholder for model " + model + ". Configure openai.api-key to enable it."
            );
        }

        try {
            String payload = buildPayload(messages, toolRegistry);
            OpenAiResponse response = transport.post(URI.create(baseUrl + "/chat/completions"), apiKey, payload, requestTimeout);
            return parseCompletion(response.body());
        } catch (ProcessingException exception) {
            throw new OpenAiTransientException("OpenAI transport failed", exception);
        } catch (IOException exception) {
            throw new OpenAiPermanentException("OpenAI request could not be prepared or parsed", exception);
        }
    }

    private String buildPayload(List<LlmMessage> messages, ToolRegistry toolRegistry) throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", model);
        payload.put("temperature", 0.0);

        ArrayNode messageArray = payload.putArray("messages");
        for (LlmMessage message : messages) {
            ObjectNode node = messageArray.addObject();
            node.put("role", normalizeRole(message.role()));
            if ("assistant".equals(message.role()) && message.toolCalls() != null && !message.toolCalls().isEmpty()) {
                ArrayNode toolCallsArray = node.putArray("tool_calls");
                for (LlmToolCall toolCall : message.toolCalls()) {
                    ObjectNode toolCallNode = toolCallsArray.addObject();
                    if (toolCall.callId() != null && !toolCall.callId().isBlank()) {
                        toolCallNode.put("id", toolCall.callId());
                    }
                    toolCallNode.put("type", "function");
                    ObjectNode function = toolCallNode.putObject("function");
                    function.put("name", toolCall.toolName());
                    function.put("arguments", objectMapper.writeValueAsString(toolCall.arguments() == null ? Map.of() : toolCall.arguments()));
                }
                if (message.content() == null) {
                    node.putNull("content");
                } else {
                    node.put("content", message.content());
                }
                continue;
            }
            if ("tool".equals(message.role()) && message.toolCallId() != null && !message.toolCallId().isBlank()) {
                node.put("tool_call_id", message.toolCallId());
            }
            if (message.content() == null) {
                node.putNull("content");
            } else {
                node.put("content", message.content());
            }
        }

        if (toolRegistry != null && !toolRegistry.tools().isEmpty()) {
            ArrayNode toolsArray = payload.putArray("tools");
            for (Tool tool : toolRegistry.tools()) {
                toolsArray.add(toolSchema(tool));
            }
            payload.put("tool_choice", "auto");
        }

        return objectMapper.writeValueAsString(payload);
    }

    private ObjectNode toolSchema(Tool tool) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "function");
        ObjectNode function = root.putObject("function");
        function.put("name", tool.name());
        function.put("description", tool.definition().description());
        function.set("parameters", parametersFor(tool));
        return root;
    }

    private ObjectNode parametersFor(Tool tool) {
        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("type", "object");
        ObjectNode properties = parameters.putObject("properties");
        ArrayNode required = parameters.putArray("required");
        parameters.put("additionalProperties", false);

        switch (tool.name()) {
            case "calculator" -> {
                properties.set("expression", stringProperty("Arithmetic expression to evaluate."));
                required.add("expression");
            }
            case "clock" -> {
                parameters.put("description", "No arguments required.");
            }
            case "web-search", "wikipedia", "knowledge-base", "conversation-search", "recall-memory" -> {
                properties.set("query", stringProperty("Search query."));
                required.add("query");
            }
            case "core-memory-upsert" -> {
                properties.set("sessionId", stringProperty("Session identifier."));
                properties.set("task", stringProperty("Memory category."));
                properties.set("value", stringProperty("Memory value to store."));
                required.add("sessionId");
                required.add("task");
                required.add("value");
            }
            default -> properties.set("value", stringProperty("Tool input."));
        }

        return parameters;
    }

    private ObjectNode stringProperty(String description) {
        ObjectNode property = objectMapper.createObjectNode();
        property.put("type", "string");
        property.put("description", description);
        return property;
    }

    private LlmCompletion parseCompletion(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return LlmCompletion.answer("");
        }
        JsonNode message = choices.get(0).path("message");
        List<LlmToolCall> toolCalls = parseToolCalls(message.path("tool_calls"));
        if (!toolCalls.isEmpty()) {
            return LlmCompletion.toolCalls(toolCalls);
        }
        String content = message.path("content").isNull() ? "" : message.path("content").asText("");
        return LlmCompletion.answer(content);
    }

    private List<LlmToolCall> parseToolCalls(JsonNode toolCallsNode) throws IOException {
        List<LlmToolCall> toolCalls = new ArrayList<>();
        if (!toolCallsNode.isArray()) {
            return toolCalls;
        }
        for (JsonNode toolCallNode : toolCallsNode) {
            String callId = toolCallNode.path("id").asText(null);
            JsonNode functionNode = toolCallNode.path("function");
            String toolName = functionNode.path("name").asText("");
            String argumentsJson = functionNode.path("arguments").asText("{}");
            Map<String, Object> arguments = parseArguments(argumentsJson);
            toolCalls.add(new LlmToolCall(toolName, arguments, callId));
        }
        return toolCalls;
    }

    private Map<String, Object> parseArguments(String argumentsJson) throws IOException {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return Map.of();
        }
        try {
            JsonNode node = objectMapper.readTree(argumentsJson);
            if (node.isObject()) {
                Map<String, Object> values = objectMapper.convertValue(node, MAP_TYPE);
                return new HashMap<>(values);
            }
        } catch (IOException ignored) {
            // fall through to string fallback
        }
        return Map.of("value", argumentsJson);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "user";
        }
        return switch (role.toLowerCase(Locale.ROOT)) {
            case "tool" -> "tool";
            case "assistant", "system", "user" -> role.toLowerCase(Locale.ROOT);
            default -> "user";
        };
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank() ? "https://api.openai.com/v1" : value.trim();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    interface OpenAiTransport {
        OpenAiResponse post(URI uri, String apiKey, String payload, Duration timeout) throws IOException;
    }

    record OpenAiResponse(int statusCode, String body) {
    }

    @ApplicationScoped
    static final class DefaultOpenAiTransport implements OpenAiTransport {
        private final OpenAiApi api;
        private final HttpClient httpClient;
        private final URI baseUri;

        @Inject
        DefaultOpenAiTransport(Config config) {
            String baseUrl = config.getOptionalValue("openai.base-url", String.class).orElse("https://api.openai.com/v1");
            int timeoutSeconds = config.getOptionalValue("openai.timeout-seconds", Integer.class).orElse(30);
            this.api = null;
            this.httpClient = buildHttpClient(timeoutSeconds);
            this.baseUri = URI.create(normalizeBaseUrl(baseUrl));
        }

        DefaultOpenAiTransport(OpenAiApi api) {
            this.api = api;
            this.httpClient = null;
            this.baseUri = URI.create("https://api.openai.com/v1");
        }

        DefaultOpenAiTransport(String baseUrl, int timeoutSeconds) {
            this.api = null;
            this.httpClient = buildHttpClient(timeoutSeconds);
            this.baseUri = URI.create(normalizeBaseUrl(baseUrl));
        }

        private HttpClient buildHttpClient(int timeoutSeconds) {
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(Math.max(5, timeoutSeconds)))
                    .build();
        }

        @Override
        public OpenAiResponse post(URI uri, String apiKey, String payload, Duration timeout) throws IOException {
            Duration effectiveTimeout = timeout == null ? Duration.ofSeconds(30) : timeout;
            Guard guard = Guard.create()
                    .withDescription("openai.transport.post")
                    .withRetry()
                    .maxRetries(2)
                    .delay(200, ChronoUnit.MILLIS)
                    .jitter(100, ChronoUnit.MILLIS)
                    .retryOn(OpenAiTransientException.class)
                    .abortOn(OpenAiPermanentException.class)
                    .done()
                    .build();

            Future<OpenAiResponse> future = TIMEOUT_EXECUTOR.submit((Callable<OpenAiResponse>) () -> guard.call(() -> postOnce(apiKey, payload, effectiveTimeout), OpenAiResponse.class));
            try {
                return future.get(Math.max(1, effectiveTimeout.toMillis()), TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException exception) {
                future.cancel(true);
                throw new OpenAiTransientException("OpenAI request timed out after " + effectiveTimeout.toMillis() + " ms", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new OpenAiTransientException("OpenAI request was interrupted", exception);
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof OpenAiTransientException transientException) {
                    throw transientException;
                }
                if (cause instanceof OpenAiPermanentException permanentException) {
                    throw permanentException;
                }
                if (cause instanceof IOException ioException) {
                    throw ioException;
                }
                throw new OpenAiTransientException("OpenAI request failed", cause);
            }
        }

        private OpenAiResponse postOnce(String apiKey, String payload, Duration effectiveTimeout) throws IOException {
            if (api != null) {
                JsonNode requestBody = new ObjectMapper().readTree(payload);
                jakarta.ws.rs.core.Response response = api.chatCompletions("Bearer " + apiKey, requestBody);
                try {
                    int status = response.getStatus();
                    String body = response.readEntity(String.class);
                    if (status < 200 || status >= 300) {
                        if (status == 429 || status >= 500) {
                            throw new OpenAiTransientException(
                                    "OpenAI request failed with status " + status + ": " + body
                            );
                        }
                        throw new OpenAiPermanentException(
                                "OpenAI request failed with status " + status + ": " + body
                        );
                    }
                    return new OpenAiResponse(status, body);
                } finally {
                    response.close();
                }
            }

            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUri.toString() + "/chat/completions"))
                    .timeout(effectiveTimeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .header("Accept", MediaType.APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new OpenAiTransientException("OpenAI request was interrupted", exception);
            }

            int status = response.statusCode();
            String body = response.body();
            if (status < 200 || status >= 300) {
                if (status == 429 || status >= 500) {
                    throw new OpenAiTransientException(
                            "OpenAI request failed with status " + status + ": " + body
                    );
                }
                throw new OpenAiPermanentException(
                        "OpenAI request failed with status " + status + ": " + body
                );
            }
            return new OpenAiResponse(status, body);
        }
    }

    @RegisterRestClient
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    interface OpenAiApi {
        @POST
        @Path("/chat/completions")
        jakarta.ws.rs.core.Response chatCompletions(@HeaderParam("Authorization") String authorization, JsonNode payload);
    }

    static final class OpenAiTransientException extends RuntimeException {
        OpenAiTransientException(String message, Throwable cause) {
            super(message, cause);
        }

        OpenAiTransientException(String message) {
            super(message);
        }
    }

    static final class OpenAiPermanentException extends RuntimeException {
        OpenAiPermanentException(String message, Throwable cause) {
            super(message, cause);
        }

        OpenAiPermanentException(String message) {
            super(message);
        }
    }
}
