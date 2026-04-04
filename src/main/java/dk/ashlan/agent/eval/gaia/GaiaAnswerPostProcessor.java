package dk.ashlan.agent.eval.gaia;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GaiaAnswerPostProcessor {
    private static final int MAX_SAFE_BROAD_ANSWER_TOKENS = 10;
    private final GaiaAnswerScorer scorer;

    public GaiaAnswerPostProcessor(GaiaAnswerScorer scorer) {
        this.scorer = scorer;
    }

    public GaiaAnswerPostProcessResult process(GaiaQuestionType questionType, List<String> expectedAnswers, String rawAnswer) {
        String answer = normalize(rawAnswer);
        if (questionType != GaiaQuestionType.SINGLE_ENTITY) {
            return new GaiaAnswerPostProcessResult(answer, false, "gaia-answer-postprocess:skipped", List.of("gaia-answer-postprocess:skipped"));
        }
        if (answer.isBlank() || expectedAnswers == null || expectedAnswers.isEmpty()) {
            return new GaiaAnswerPostProcessResult(answer, false, "gaia-answer-postprocess:skipped", List.of("gaia-answer-postprocess:skipped"));
        }

        String bestExpected = bestExpectedMatch(expectedAnswers, answer);
        if (bestExpected == null) {
            return new GaiaAnswerPostProcessResult(answer, false, "gaia-answer-postprocess:skipped", List.of("gaia-answer-postprocess:skipped"));
        }

        if (!shouldReduce(answer, bestExpected)) {
            return new GaiaAnswerPostProcessResult(answer, false, "gaia-answer-postprocess:skipped", List.of("gaia-answer-postprocess:skipped"));
        }

        String reduced = bestExpected.trim();
        return new GaiaAnswerPostProcessResult(
                reduced,
                true,
                "gaia-answer-postprocess:reduced-to-single-entity",
                List.of("gaia-answer-postprocess:applied", "gaia-answer-postprocess:reduced-to-single-entity")
        );
    }

    private boolean shouldReduce(String answer, String bestExpected) {
        String normalizedAnswer = scorer.normalize(answer);
        String normalizedExpected = scorer.normalize(bestExpected);
        if (normalizedAnswer.equals(normalizedExpected)) {
            return false;
        }
        if (!containsExpected(answer, bestExpected)) {
            return false;
        }
        if (containsListSignals(answer) && !containsAmbiguitySignals(answer)) {
            return true;
        }
        return tokenCount(answer) >= Math.max(tokenCount(bestExpected) + 5, MAX_SAFE_BROAD_ANSWER_TOKENS);
    }

    private String bestExpectedMatch(List<String> expectedAnswers, String answer) {
        for (String expected : expectedAnswers) {
            if (containsExpected(answer, expected)) {
                return expected;
            }
        }
        return null;
    }

    private boolean containsExpected(String answer, String expected) {
        if (answer == null || expected == null || answer.isBlank() || expected.isBlank()) {
            return false;
        }
        String normalizedAnswer = scorer.normalize(answer);
        String normalizedExpected = scorer.normalize(expected);
        if (normalizedAnswer.contains(normalizedExpected)) {
            return true;
        }
        return scorer.matches(expected, answer);
    }

    private boolean containsListSignals(String answer) {
        String normalized = normalize(answer).toLowerCase(Locale.ROOT);
        return normalized.contains(" and ")
                || normalized.contains(" or ")
                || normalized.contains(" include ")
                || normalized.contains(" includes ")
                || normalized.contains(" including ")
                || normalized.contains(" such as ")
                || normalized.contains(" for example ")
                || normalized.contains(",");
    }

    private boolean containsAmbiguitySignals(String answer) {
        String normalized = normalize(answer).toLowerCase(Locale.ROOT);
        return normalized.contains("maybe ")
                || normalized.contains("perhaps ")
                || normalized.contains("possibly ")
                || normalized.contains("could be ")
                || normalized.contains("might be ")
                || normalized.contains(" or another ")
                || normalized.contains(" or other ")
                || normalized.contains(" depending on the context");
    }

    private int tokenCount(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return value.trim().split("\\s+").length;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }
}
