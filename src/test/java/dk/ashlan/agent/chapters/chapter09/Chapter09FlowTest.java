package dk.ashlan.agent.chapters.chapter09;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter09FlowTest {
    @Test
    void coordinatorRoutesAndReviewerEvaluates() {
        var research = Chapter09Support.researchFlow();
        assertEquals("research", research.agentName());
        assertTrue(research.approved());

        var coding = Chapter09Support.codingFlow();
        assertEquals("coding", coding.agentName());
        assertTrue(coding.review().length() > 0);
    }
}
