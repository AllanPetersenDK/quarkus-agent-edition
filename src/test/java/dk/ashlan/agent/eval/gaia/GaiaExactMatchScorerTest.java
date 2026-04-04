package dk.ashlan.agent.eval.gaia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GaiaExactMatchScorerTest {
    @Test
    void matchesAfterNormalizingCasePunctuationAndCommonAnswerWrappers() {
        GaiaExactMatchScorer scorer = new GaiaExactMatchScorer();

        assertTrue(scorer.matches("PostgreSQL", "The answer is PostgreSQL."));
        assertTrue(scorer.matches("H2", "answer: h2"));
        assertFalse(scorer.matches("PostgreSQL", "My answer is H2"));
    }

    @Test
    void normalizeStripsWrappersAndWhitespace() {
        GaiaExactMatchScorer scorer = new GaiaExactMatchScorer();

        assertEquals("postgresql", scorer.normalize("  Final answer: PostgreSQL.  "));
    }
}
