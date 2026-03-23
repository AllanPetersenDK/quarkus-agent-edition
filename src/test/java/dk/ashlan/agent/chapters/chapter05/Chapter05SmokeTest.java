package dk.ashlan.agent.chapters.chapter05;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter05SmokeTest {
    @Test
    void chapter05DemosWork() {
        assertTrue(new RagIngestionDemo().run("arithmetic").contains("calculator"));
        assertTrue(new KnowledgeBaseToolDemo().run("Quarkus").contains("Quarkus"));
    }
}
