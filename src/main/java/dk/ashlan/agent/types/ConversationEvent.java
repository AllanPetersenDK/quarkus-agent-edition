package dk.ashlan.agent.types;

import java.time.Instant;

public sealed interface ConversationEvent permits MessageEvent, ToolCallEvent, ToolResultEvent, SystemEvent {
    EventType type();

    String details();

    Instant timestamp();
}
