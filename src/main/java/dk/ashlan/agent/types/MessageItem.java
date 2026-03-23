package dk.ashlan.agent.types;

public record MessageItem(String role, String content) implements ContentItem {
}
