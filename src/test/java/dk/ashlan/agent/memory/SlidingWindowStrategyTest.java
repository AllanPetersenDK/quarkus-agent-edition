package dk.ashlan.agent.memory;

import dk.ashlan.agent.llm.LlmMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlidingWindowStrategyTest {
    @Test
    void keepsOnlyWindowSizedTail() {
        SlidingWindowStrategy strategy = new SlidingWindowStrategy();
        List<String> memory = List.of("1", "2", "3", "4", "5");

        List<String> updated = strategy.update(memory, "6");

        assertEquals(5, updated.size());
        assertEquals("2", updated.get(0));
        assertEquals("6", updated.get(4));
    }

    @Test
    void trimsConversationWhileKeepingTheLeadingSystemPrompt() {
        SlidingWindowStrategy strategy = new SlidingWindowStrategy();
        List<LlmMessage> messages = List.of(
                LlmMessage.system("You are helpful."),
                LlmMessage.user("one"),
                LlmMessage.assistant("two"),
                LlmMessage.tool("calculator", "three"),
                LlmMessage.user("four"),
                LlmMessage.assistant("five"),
                LlmMessage.user("six")
        );

        List<LlmMessage> trimmed = strategy.trimConversation(messages, 4);

        assertEquals(4, trimmed.size());
        assertEquals("system", trimmed.get(0).role());
        assertEquals("You are helpful.", trimmed.get(0).content());
        assertTrue(trimmed.stream().anyMatch(message -> "user".equals(message.role()) && "six".equals(message.content())));
    }
}
