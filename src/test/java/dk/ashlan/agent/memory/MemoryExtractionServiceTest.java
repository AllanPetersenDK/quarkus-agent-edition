package dk.ashlan.agent.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryExtractionServiceTest {
    @Test
    void structuredExtractionProducesProblemSolvingFields() {
        MemoryExtractionService service = new MemoryExtractionService();

        MemoryExtractionResult result = service.extract(
                "session-1",
                "goal",
                "Remember that my favorite database is PostgreSQL."
        );

        assertEquals(MemoryWriteDecision.ADD, result.decision());
        assertNotNull(result.memory());
        assertTrue(result.memory().problem().contains("preference"));
        assertTrue(result.memory().approach().contains("preference"));
        assertTrue(result.memory().result().contains("PostgreSQL"));
    }

    @Test
    void afterRunSignalsKeepCompactAnswerFields() {
        MemoryExtractionService service = new MemoryExtractionService();

        MemoryExtractionResult result = service.extract(
                "session-1",
                "goal",
                "What is 25 * 4? => 25 * 4 = 100 | trace: iteration:1"
        );

        assertEquals(MemoryWriteDecision.ADD, result.decision());
        assertNotNull(result.memory());
        assertTrue(result.memory().problem().contains("What is 25 * 4?"));
        assertTrue(result.memory().summary().contains("What is 25 * 4?"));
        assertTrue(result.memory().result().contains("100"));
        assertTrue(result.memory().approach().contains("after-run"));
    }

    @Test
    void profileSignalsAndGenericNoiseAreHandledConservatively() {
        MemoryExtractionService service = new MemoryExtractionService();

        MemoryExtractionResult profile = service.extract(
                "session-1",
                "profile",
                "I am a data engineer."
        );
        MemoryExtractionResult noise = service.extract(
                "session-1",
                "profile",
                "thanks"
        );

        assertEquals(MemoryWriteDecision.ADD, profile.decision());
        assertNotNull(profile.memory());
        assertTrue(profile.memory().problem().contains("profile"));
        assertTrue(profile.memory().result().contains("data engineer"));
        assertEquals(MemoryWriteDecision.SKIP, noise.decision());
    }

    @Test
    void rememberSignalsStayCompactAndStripTraceNoise() {
        MemoryExtractionService service = new MemoryExtractionService();

        MemoryExtractionResult result = service.extract(
                "session-1",
                "Remember that I prefer concise answers and PostgreSQL.",
                "Remember that I prefer concise answers and PostgreSQL. => Got it! I will keep answers concise and focus on PostgreSQL when relevant. | trace: iteration:1"
        );

        assertEquals(MemoryWriteDecision.ADD, result.decision());
        assertNotNull(result.memory());
        assertTrue(result.memory().task().contains("Remember that I prefer concise answers and PostgreSQL."));
        assertTrue(result.memory().approach().contains("after-run"));
        assertTrue(result.memory().result().contains("Got it!"));
        assertTrue(result.memory().memory().contains("Problem:"));
    }
}
