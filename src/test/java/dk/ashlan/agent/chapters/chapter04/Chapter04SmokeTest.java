package dk.ashlan.agent.chapters.chapter04;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter04SmokeTest {
    @Test
    void chapter04DemosWork() {
        assertTrue(new SolveKipchogeProblemDemo().run().contains("100"));
        assertTrue(new AgentStructuredOutputDemo().run().contains("structured output demo"));
        assertTrue(new HumanInTheLoopDemo().run().contains("before:"));
    }
}
