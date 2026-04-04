package dk.ashlan.agent.chapters.chapter07;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter07FlowTest {
    @Test
    void planningAndReflectionImprovesThinOutput() {
        var plan = Chapter07Support.plan("Explain Quarkus agents");
        assertEquals(3, plan.steps().size());
        assertTrue(plan.formattedPlan().contains("Task plan"));
        assertTrue(plan.formattedPlan().contains("Goal: Explain Quarkus agents"));
        assertTrue(plan.formattedPlan().contains("done when:"));

        var initial = Chapter07Support.reflect("short");
        assertFalse(initial.accepted());
        assertTrue(initial.feedback().contains("too thin"));

        var improved = Chapter07Support.orchestrator().run("Explain Quarkus agents");
        assertTrue(improved.finalAnswer().contains("fuller answer"));
    }
}
