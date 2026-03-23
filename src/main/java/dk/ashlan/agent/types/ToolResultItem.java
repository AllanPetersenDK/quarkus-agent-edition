package dk.ashlan.agent.types;

public record ToolResultItem(String toolName, String result, boolean success) implements ContentItem {
}
