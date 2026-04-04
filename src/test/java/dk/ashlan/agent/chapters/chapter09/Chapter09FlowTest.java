package dk.ashlan.agent.chapters.chapter09;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter09FlowTest {
    @Test
    void coordinatorRoutesAndReviewerEvaluates() {
        var research = Chapter09Support.researchFlow();
        assertNotNull(research.runId());
        assertNotNull(research.createdAt());
        assertEquals("research the Quarkus agent edition", research.objective());
        assertEquals("research", research.agentName());
        assertTrue(research.approved());
        assertTrue(research.routeReason().contains("research"));
        assertTrue(research.coordinatorSummary().contains("reviewer"));
        assertTrue(research.traceEntries().contains("chapter9-route:research"));

        var coding = Chapter09Support.codingFlow();
        assertNotNull(coding.runId());
        assertEquals("coding", coding.agentName());
        assertTrue(coding.review().contains("Approved"));
        assertTrue(coding.routeReason().contains("coding"));
        assertTrue(coding.traceEntries().contains("chapter9-route:coding"));

        var fallback = Chapter09Support.coordinator().run("help with the agent example");
        assertEquals("research", fallback.agentName());
        assertTrue(fallback.routeReason().contains("fallback"));

        var rejected = Chapter09Support.coordinator().run("x");
        assertFalse(rejected.approved());
        assertTrue(rejected.review().contains("Rejected"));
        assertTrue(rejected.traceEntries().contains("chapter9-review:rejected"));
    }
}
