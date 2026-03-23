package dk.ashlan.agent.types;

import java.time.Instant;

public record MessageEvent(String role, String content, Instant timestamp) implements ConversationEvent {
    @Override
    public EventType type() {
        return EventType.MESSAGE;
    }

    @Override
    public String details() {
        return role + ":" + content;
    }
}
