package dk.ashlan.agent.chapters.chapter08;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter08FlowTest {
    @Test
    void workspaceSafetyAndAgentFlowWork() {
        assertTrue(Chapter08Support.pathTraversalRejected());
        assertTrue(Chapter08Support.orchestrator().run("Write a response").contains("Workspace:"));
        assertThrows(IllegalArgumentException.class, () -> Chapter08Support.workspaceService().resolve("../../secret"));
    }
}
