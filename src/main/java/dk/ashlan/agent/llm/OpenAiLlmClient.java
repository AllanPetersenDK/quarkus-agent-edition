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
import org.eclipse.microprofile.config.Config;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class OpenAiLlmClient implements BaseLlmClient {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final OpenAiTransport transport;
    private final ObjectMapper objectMapper;

    public OpenAiLlmClient(
            Config config
    ) {
        this(
                config.getOptionalValue("openai.api-key", String.class).orElse(""),
                config.getOptionalValue("openai.model", String.class).orElse("gpt-4.1-mini"),
                config.getOptionalValue("openai.base-url", String.class).orElse("https://api.openai.com/v1"),
                config.getOptionalValue("openai.timeout-seconds", Integer.class).orElse(30),
                new DefaultOpenAiTransport(config.getOptionalValue("openai.timeout-seconds", Integer.class).orElse(30)),
                new ObjectMapper()
        );
    }

    OpenAiLlmClient(String apiKey, String model, String baseUrl, int timeoutSeconds, ObjectMapper objectMapper) {
        this(apiKey, model, baseUrl, timeoutSeconds, new DefaultOpenAiTransport(timeoutSeconds), objectMapper);
    }

    OpenAiLlmClient(String apiKey, String model, String baseUrl, int timeoutSeconds, OpenAiTransport transport, ObjectMapper objectMapper) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "gpt-4.1-mini" : model.trim();
        this.baseUrl = normalizeBaseUrl(baseUrl);
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
            OpenAiResponse response = transport.post(URI.create(baseUrl + "/chat/completions"), apiKey, buildPayload(messages, toolRegistry), Duration.ofSeconds(60));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "OpenAI request failed with status " + response.statusCode() + ": " + response.body()
                );
            }
            return parseCompletion(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI request interrupted", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("OpenAI request failed", exception);
        }
    }

    private String buildPayload(List<LlmMessage> messages, ToolRegistry toolRegistry) throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", model);
        payload.put("temperature", 0.0);
        payload.put("tool_choice", "auto");

        ArrayNode messageArray = payload.putArray("messages");
        for (LlmMessage message : messages) {
            ObjectNode node = messageArray.addObject();
            node.put("role", normalizeRole(message.role()));
            if (message.name() != null && "tool".equals(message.role())) {
                node.put("content", "Tool result from " + message.name() + ": " + message.content());
            } else {
                node.put("content", message.content() == null ? "" : message.content());
            }
        }

        if (toolRegistry != null && !toolRegistry.definitions().isEmpty()) {
            ArrayNode toolsArray = payload.putArray("tools");
            for (Tool tool : toolRegistry.tools()) {
                toolsArray.add(toolSchema(tool));
            }
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
            case "web-search", "wikipedia", "knowledge-base", "conversation-search" -> {
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
            JsonNode functionNode = toolCallNode.path("function");
            String toolName = functionNode.path("name").asText("");
            String argumentsJson = functionNode.path("arguments").asText("{}");
            Map<String, Object> arguments = parseArguments(argumentsJson);
            toolCalls.add(new LlmToolCall(toolName, arguments));
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
            case "tool" -> "system";
            case "assistant", "system", "user" -> role.toLowerCase(Locale.ROOT);
            default -> "user";
        };
    }

    private String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank() ? "https://api.openai.com/v1" : value.trim();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    interface OpenAiTransport {
        OpenAiResponse post(URI uri, String apiKey, String payload, Duration timeout) throws IOException, InterruptedException;
    }

    record OpenAiResponse(int statusCode, String body) {
    }

    static final class DefaultOpenAiTransport implements OpenAiTransport {
        private final java.net.http.HttpClient httpClient;

        DefaultOpenAiTransport(int timeoutSeconds) {
            this.httpClient = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(Math.max(5, timeoutSeconds)))
                    .build();
        }

        @Override
        public OpenAiResponse post(URI uri, String apiKey, String payload, Duration timeout) throws IOException, InterruptedException {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            return new OpenAiResponse(response.statusCode(), response.body());
        }
    }
}
