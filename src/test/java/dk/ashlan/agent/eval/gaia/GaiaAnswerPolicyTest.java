package dk.ashlan.agent.eval.gaia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GaiaAnswerPolicyTest {
    @Test
    void singleEntityQuestionsGetAStrictPolicyMessage() {
        GaiaAnswerPolicy policy = new GaiaAnswerPolicy();

        assertTrue(policy.supplementalMessages(GaiaQuestionType.SINGLE_ENTITY).getFirst().content().contains("exactly one best answer"));
        assertTrue(policy.supplementalMessages(GaiaQuestionType.GENERAL).isEmpty());
    }
}
