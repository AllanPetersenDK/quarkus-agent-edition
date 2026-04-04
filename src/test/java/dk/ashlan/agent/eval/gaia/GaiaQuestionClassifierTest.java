package dk.ashlan.agent.eval.gaia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GaiaQuestionClassifierTest {
    @Test
    void classifiesEntityNumericAndListQuestions() {
        GaiaQuestionClassifier classifier = new GaiaQuestionClassifier();

        assertEquals(GaiaQuestionType.SINGLE_ENTITY, classifier.classify("What species is shown?"));
        assertEquals(GaiaQuestionType.NUMERIC, classifier.classify("How many layers does the model have?"));
        assertEquals(GaiaQuestionType.LIST_OR_SET, classifier.classify("What page numbers are in the comma-delimited list?"));
        assertEquals(GaiaQuestionType.GENERAL, classifier.classify("Explain the result."));
    }
}
