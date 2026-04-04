package dk.ashlan.agent.eval.gaia;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GaiaAnswerPostProcessorTest {
    @Test
    void reducesBroadListLikeSingleEntityAnswerWhenExpectedMatchIsClear() {
        GaiaAnswerPostProcessor postProcessor = new GaiaAnswerPostProcessor(new GaiaAnswerScorer());

        GaiaAnswerPostProcessResult result = postProcessor.process(
                GaiaQuestionType.SINGLE_ENTITY,
                List.of("Rockhopper penguin"),
                "The bird species include the rockhopper penguin and the kakapo."
        );

        assertTrue(result.applied());
        assertEquals("Rockhopper penguin", result.answer());
        assertTrue(result.traceEvents().contains("gaia-answer-postprocess:applied"));
        assertTrue(result.traceEvents().contains("gaia-answer-postprocess:reduced-to-single-entity"));
    }

    @Test
    void doesNotAggressivelyReduceUnclearAnswers() {
        GaiaAnswerPostProcessor postProcessor = new GaiaAnswerPostProcessor(new GaiaAnswerScorer());

        GaiaAnswerPostProcessResult result = postProcessor.process(
                GaiaQuestionType.SINGLE_ENTITY,
                List.of("PostgreSQL"),
                "Maybe PostgreSQL or another database, depending on the context."
        );

        assertFalse(result.applied());
        assertEquals("Maybe PostgreSQL or another database, depending on the context.", result.answer());
    }
}
