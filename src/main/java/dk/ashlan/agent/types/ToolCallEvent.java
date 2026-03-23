package dk.ashlan.agent.types;

import java.time.Instant;
import java.util.Map;

public record ToolCallEvent(String toolName, Map<String, Object> arguments, Instant timestamp) implements ConversationEvent {
    @Override
    public EventType type() {
        return EventType.TOOL_CALL;
    }

    @Override
    public String details() {
        return toolName + ":" + arguments;
    }
}
