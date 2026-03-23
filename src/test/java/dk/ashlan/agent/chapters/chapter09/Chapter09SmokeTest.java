package dk.ashlan.agent.chapters.chapter09;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter09SmokeTest {
    @Test
    void chapter09DemosWork() {
        assertTrue(new ResearchCoordinationDemo().run().output().contains("Research summary"));
        assertTrue(new CodingCoordinationDemo().run().output().contains("Coding plan"));
        assertTrue(new ReviewerDemo().run("thin").contains("thin"));
    }
}
