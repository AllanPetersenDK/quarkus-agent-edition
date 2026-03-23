package dk.ashlan.agent.types;

import java.time.Instant;

public record SystemEvent(String details, Instant timestamp) implements ConversationEvent {
    @Override
    public EventType type() {
        return EventType.SYSTEM;
    }
}
