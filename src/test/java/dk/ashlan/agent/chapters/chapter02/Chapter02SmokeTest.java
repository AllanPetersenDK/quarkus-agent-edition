package dk.ashlan.agent.chapters.chapter02;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter02SmokeTest {
    @Test
    void chapter02DemosWork() {
        assertTrue(new LlmChatDemo().run("hello").content().contains("hello"));
        ConversationManagementDemo conversationDemo = new ConversationManagementDemo();
        conversationDemo.add("hello");
        assertEquals(1, conversationDemo.messages().size());
        assertFalse(conversationDemo.buildPrompt("question", Chapter02Support.memoryService()).isEmpty());
        assertEquals("ok", new StructuredOutputDemo().run("answer: ok"));
        assertTrue(new StructuredOutputDemo().formatToolCall("calculator").contains("tool:calculator"));
        assertTrue(new AsyncLlmCallDemo().runAsync("hi").join().content().contains("hi"));
        assertTrue(new PotatoProblemDemo().solve("").contains("potato"));
    }
}
