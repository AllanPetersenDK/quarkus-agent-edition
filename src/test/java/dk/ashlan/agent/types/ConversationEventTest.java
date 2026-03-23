package dk.ashlan.agent.types;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationEventTest {
    @Test
    void eventKindsAndDetailsAreAvailable() {
        MessageEvent messageEvent = new MessageEvent("user", "hello", Instant.EPOCH);
        ToolCallEvent toolCallEvent = new ToolCallEvent("calculator", Map.of("expression", "25 * 4"), Instant.EPOCH);
        ToolResultEvent toolResultEvent = new ToolResultEvent("calculator", "100", true, Instant.EPOCH);
        SystemEvent systemEvent = new SystemEvent("boot", Instant.EPOCH);

        assertEquals(EventType.MESSAGE, messageEvent.type());
        assertEquals(EventType.TOOL_CALL, toolCallEvent.type());
        assertEquals(EventType.TOOL_RESULT, toolResultEvent.type());
        assertEquals(EventType.SYSTEM, systemEvent.type());
    }
}
