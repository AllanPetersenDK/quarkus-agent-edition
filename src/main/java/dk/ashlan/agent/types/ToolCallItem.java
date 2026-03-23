package dk.ashlan.agent.types;

import java.util.Map;

public record ToolCallItem(String toolName, Map<String, Object> arguments) implements ContentItem {
}
