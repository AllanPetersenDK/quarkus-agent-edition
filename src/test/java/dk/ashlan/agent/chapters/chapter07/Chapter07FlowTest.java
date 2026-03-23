package dk.ashlan.agent.chapters.chapter07;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter07FlowTest {
    @Test
    void planningAndReflectionImprovesThinOutput() {
        var plan = Chapter07Support.plan("Explain Quarkus agents");
        assertEquals(3, plan.steps().size());

        var initial = Chapter07Support.reflect("short");
        assertTrue(!initial.accepted());

        var improved = Chapter07Support.orchestrator().run("Explain Quarkus agents");
        assertTrue(improved.finalAnswer().contains("fuller answer"));
    }
}
