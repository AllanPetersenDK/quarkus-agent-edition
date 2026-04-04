package dk.ashlan.agent.memory;

import dk.ashlan.agent.llm.LlmMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SummarizationStrategyTest {
    @Test
    void summarizesIntoSingleEntry() {
        SummarizationStrategy strategy = new SummarizationStrategy();

        List<String> updated = strategy.update(List.of("hello"), "world");

        assertTrue(updated.get(0).contains("hello"));
        assertTrue(updated.get(0).contains("world"));
    }

    @Test
    void summarizesConversationIntoCompactRoleAwareText() {
        SummarizationStrategy strategy = new SummarizationStrategy();

        String summary = strategy.summarizeConversation(List.of(
                LlmMessage.system("You are helpful."),
                LlmMessage.user("Remember PostgreSQL"),
                LlmMessage.assistant("Noted."),
                LlmMessage.tool("memory", "Stored memory for session chapter-6"),
                LlmMessage.user("What did you remember?")
        ));

        assertTrue(summary.contains("user: Remember PostgreSQL"));
        assertTrue(summary.contains("assistant: Noted."));
        assertTrue(summary.contains("tool: Stored memory for session chapter-6"));
    }
}
