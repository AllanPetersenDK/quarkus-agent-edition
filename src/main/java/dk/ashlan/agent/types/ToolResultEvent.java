package dk.ashlan.agent.types;

import java.time.Instant;

public record ToolResultEvent(String toolName, String result, boolean success, Instant timestamp) implements ConversationEvent {
    @Override
    public EventType type() {
        return EventType.TOOL_RESULT;
    }

    @Override
    public String details() {
        return toolName + ":" + result;
    }
}
