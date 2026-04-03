package dk.ashlan.agent.chapters.chapter07.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LangChain4jToolCallingCompanionDemoTest {
    @Test
    void runDelegatesToTheFrameworkToolCallingAssistantWhenPresent() {
        LangChain4jToolCallingCompanionDemo demo = new LangChain4jToolCallingCompanionDemo(request -> "Tool-calling answer for " + request);

        assertEquals("Tool-calling answer for calculator", demo.run("calculator"));
    }
}
