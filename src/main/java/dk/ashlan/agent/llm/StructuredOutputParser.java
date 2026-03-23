package dk.ashlan.agent.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StructuredOutputParser {
    public LlmCompletion parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return LlmCompletion.answer("");
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("tool:")) {
            String[] parts = trimmed.substring("tool:".length()).split("\\|", 2);
            String toolName = parts[0].trim();
            Map<String, Object> arguments = parts.length > 1 ? Map.of("value", parts[1].trim()) : Map.of();
            return LlmCompletion.toolCalls(List.of(new LlmToolCall(toolName, arguments)));
        }
        if (trimmed.startsWith("answer:")) {
            return LlmCompletion.answer(trimmed.substring("answer:".length()).trim());
        }
        return LlmCompletion.answer(trimmed);
    }

    public String formatToolCall(String toolName, Map<String, Object> arguments) {
        List<String> parts = new ArrayList<>();
        arguments.forEach((key, value) -> parts.add(key + "=" + value));
        return "tool:" + toolName + (parts.isEmpty() ? "" : "|" + String.join(",", parts));
    }
}
