package dk.ashlan.agent.chapters.chapter07;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter07SmokeTest {
    @Test
    void chapter07DemosWork() {
        assertTrue(new PlanningDemo().run("Explain Quarkus agents").steps().size() >= 3);
        assertTrue(new ReflectionDemo().run("short").accepted() == false);
        assertTrue(new ImprovementLoopDemo().run("Explain Quarkus agents").contains("FINAL_ANSWER"));
    }
}
