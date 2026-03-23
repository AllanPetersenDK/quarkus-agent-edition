package dk.ashlan.agent.types;

public sealed interface ContentItem permits MessageItem, ToolCallItem, ToolResultItem {
}
