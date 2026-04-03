package dk.ashlan.agent.chapters.chapter07.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LangChain4jAgenticCompanionDemoTest {
    @Test
    void runDelegatesToTheFrameworkWorkflowWhenPresent() {
        LangChain4jAgenticCompanionDemo demo = new LangChain4jAgenticCompanionDemo(topic -> "Plan for " + topic);

        assertEquals("Plan for refactor", demo.run("refactor"));
    }
}
