package dk.ashlan.agent.llm;

import dk.ashlan.agent.core.ExecutionContext;
import dk.ashlan.agent.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangChain4jLlmClientTest {
    @Test
    void completeDelegatesToTheFrameworkAssistant() {
        AtomicReference<String> prompt = new AtomicReference<>();
        LangChain4jCompanionAssistant assistant = request -> {
            prompt.set(request);
            return "Framework answer";
        };
        LangChain4jLlmClient client = new LangChain4jLlmClient(assistant);

        LlmCompletion completion = client.complete(
                List.of(LlmMessage.system("system"), LlmMessage.user("What is the comparison path?")),
                new ToolRegistry(List.of()),
                new ExecutionContext("What is the comparison path?", "session-1")
        );

        assertEquals("Framework answer", completion.content());
        assertTrue(prompt.get().contains("session-1"));
        assertTrue(prompt.get().contains("What is the comparison path?"));
        assertTrue(prompt.get().contains("LangChain4j-backed"));
    }
}
