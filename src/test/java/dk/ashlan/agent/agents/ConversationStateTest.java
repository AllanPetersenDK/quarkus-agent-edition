package dk.ashlan.agent.agents;

import dk.ashlan.agent.types.Event;
import dk.ashlan.agent.types.MessageItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationStateTest {
    @Test
    void conversationStateCollectsEventsAndContent() {
        ConversationState state = new ConversationState();
        state.addContent(new MessageItem("user", "hello"));
        state.addEvent(Event.of("user-message", "hello"));

        assertEquals(1, state.contents().size());
        assertEquals(1, state.events().size());
    }
}
