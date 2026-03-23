package dk.ashlan.agent.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmModelTest {
    @Test
    void requestAndResponseBasicsWork() {
        LlmRequest request = LlmRequest.of("hello");
        LlmResponse response = LlmResponse.answer("world");

        assertEquals(1, request.messages().size());
        assertTrue(response.toolCalls().isEmpty());
        assertEquals("world", response.content());
    }
}
