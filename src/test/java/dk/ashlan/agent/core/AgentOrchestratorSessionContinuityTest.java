package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentOrchestratorSessionContinuityTest {
    @Test
    void remembersUserFactsAcrossRunsForTheSameSession() {
        SessionManager sessionManager = new SessionManager();
        ToolRegistry toolRegistry = new ToolRegistry(List.of());
        AtomicReference<List<LlmMessage>> secondRequest = new AtomicReference<>();
        LlmClient llmClient = (messages, registry, context) -> {
            boolean asksAboutFavoriteDatabase = messages.stream().anyMatch(message ->
                    "user".equals(message.role())
                            && "What is my favorite database?".equals(message.content())
            );
            if (asksAboutFavoriteDatabase) {
                if ("ch4-context-1".equals(context.getSessionId())) {
                    secondRequest.set(List.copyOf(messages));
                    boolean rememberedFact = messages.stream().anyMatch(message ->
                            "user".equals(message.role())
                                    && "Remember that my favorite database is PostgreSQL.".equals(message.content())
                    );
                    boolean rememberedAssistant = messages.stream().anyMatch(message ->
                            "assistant".equals(message.role())
                                    && "Acknowledged. I will remember PostgreSQL.".equals(message.content())
                    );
                    if (rememberedFact && rememberedAssistant) {
                        return LlmCompletion.answer("Your favorite database is PostgreSQL.");
                    }
                }
                return LlmCompletion.answer("I do not know your favorite database.");
            }
            return LlmCompletion.answer("Acknowledged. I will remember PostgreSQL.");
        };

        AgentOrchestrator orchestrator = new AgentOrchestrator(
                llmClient,
                toolRegistry,
                new ToolExecutor(toolRegistry),
                new MemoryService(sessionManager, new InMemoryTaskMemoryStore(), new MemoryExtractionService()),
                sessionManager,
                3,
                "system prompt"
        );

        AgentRunResult first = orchestrator.run(
                "Remember that my favorite database is PostgreSQL.",
                "ch4-context-1"
        );
        AgentRunResult second = orchestrator.run(
                "What is my favorite database?",
                "ch4-context-1"
        );
        AgentRunResult isolated = orchestrator.run(
                "What is my favorite database?",
                "ch4-context-2"
        );

        assertTrue(first.finalAnswer().contains("remember PostgreSQL"));
        assertTrue(second.finalAnswer().contains("PostgreSQL"));
        assertTrue(isolated.finalAnswer().contains("I do not know"));
        assertEquals(1L, secondRequest.get().stream()
                .filter(message -> "user".equals(message.role())
                        && "What is my favorite database?".equals(message.content()))
                .count());
        assertTrue(secondRequest.get().stream().anyMatch(message ->
                "assistant".equals(message.role())
                        && "Acknowledged. I will remember PostgreSQL.".equals(message.content())
        ));
    }
}
