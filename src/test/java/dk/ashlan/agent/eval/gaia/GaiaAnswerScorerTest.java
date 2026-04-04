package dk.ashlan.agent.eval.gaia;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GaiaAnswerScorerTest {
    @Test
    void exactMatchAndNormalizationPass() {
        GaiaAnswerScorer scorer = new GaiaAnswerScorer();

        GaiaScoreResult exact = scorer.score("PostgreSQL", "PostgreSQL");
        GaiaScoreResult normalized = scorer.score("PostgreSQL", "  Final answer: PostgreSQL. ");

        assertTrue(exact.passed());
        assertTrue(normalized.passed());
        assertEquals("normalized-exact-match", exact.reason());
        assertEquals("postgresql", normalized.expectedNormalized());
    }

    @Test
    void numericEquivalencePasses() {
        GaiaAnswerScorer scorer = new GaiaAnswerScorer();

        GaiaScoreResult score = scorer.score("1000", "1,000.00");

        assertTrue(score.passed());
        assertEquals("numeric-equivalence", score.reason());
    }

    @Test
    void shortAnswerFallbackDoesNotCreateFalsePositive() {
        GaiaAnswerScorer scorer = new GaiaAnswerScorer();

        GaiaScoreResult falsePositive = scorer.score("Rome", "Romeo");
        GaiaScoreResult supported = scorer.score(List.of("H2", "Hydrogen"), "The answer is H2.");
        GaiaScoreResult pluralMatch = scorer.score("Rockhopper penguin", "Rockhopper penguins are featured.");

        assertFalse(falsePositive.passed());
        assertTrue(supported.passed());
        assertTrue(pluralMatch.passed());
        assertEquals("H2", supported.matchedExpected());
    }

    @Test
    void entityFocusedPromptRejectsCompetingListAnswers() {
        GaiaAnswerScorer scorer = new GaiaAnswerScorer();

        GaiaScoreResult exactEntity = scorer.score(
                "Which species does the video show?",
                List.of("Rockhopper penguin"),
                "Rockhopper penguin"
        );
        GaiaScoreResult compactEntity = scorer.score(
                "Which species does the video show?",
                List.of("Rockhopper penguin"),
                "The answer is Rockhopper penguin."
        );
        GaiaScoreResult competingList = scorer.score(
                "Which species does the video show?",
                List.of("Rockhopper penguin"),
                "The bird species include the rockhopper penguin and the kakapo."
        );

        assertTrue(exactEntity.passed());
        assertTrue(compactEntity.passed());
        assertFalse(competingList.passed());
        assertEquals("entity-list-penalty", competingList.reason());
    }

    @Test
    void definitionPromptStillAllowsShortExplanatoryAnswers() {
        GaiaAnswerScorer scorer = new GaiaAnswerScorer();

        GaiaScoreResult score = scorer.score(
                "What is PostgreSQL?",
                List.of("PostgreSQL"),
                "PostgreSQL is an open-source relational database."
        );

        assertTrue(score.passed());
        assertEquals("short-answer-match", score.reason());
    }
}
