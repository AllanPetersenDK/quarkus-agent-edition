package dk.ashlan.agent.eval.gaia;

import dk.ashlan.agent.llm.LlmMessage;

import java.util.List;

public class GaiaAnswerPolicy {
    public List<LlmMessage> supplementalMessages(GaiaQuestionType questionType) {
        if (questionType != GaiaQuestionType.SINGLE_ENTITY) {
            return List.of();
        }
        return List.of(LlmMessage.system("""
                GAIA task policy: return exactly one best answer.
                Do not provide multiple candidates.
                Do not answer with lists unless the question explicitly asks for a list.
                If the question asks for a species, person, company, source, city, country, or other single named entity, provide only the single best matching entity.
                """.trim()));
    }
}
