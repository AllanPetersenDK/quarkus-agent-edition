package dk.ashlan.agent.llm;

import java.util.Map;

public record LlmToolCall(String toolName, Map<String, Object> arguments) {
}
