package dk.ashlan.agent.chapters.chapter08;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter08SmokeTest {
    @Test
    void chapter08DemosWork() {
        assertTrue(new WorkspaceSafetyDemo().run());
        assertTrue(new WorkspaceRoundTripDemo().run().contains("hello workspace"));
        assertTrue(new CodeAgentDemo().run("Generate a file").contains("Chapter 8 workspace response"));
    }
}
