package dk.ashlan.agent.eval.gaia;

import java.util.Locale;

public class GaiaQuestionClassifier {
    public GaiaQuestionType classify(String prompt) {
        String normalized = normalize(prompt);
        if (normalized.isBlank()) {
            return GaiaQuestionType.GENERAL;
        }
        if (matchesAny(normalized,
                "how many",
                "what number",
                "difference between")) {
            return GaiaQuestionType.NUMERIC;
        }
        if (matchesAny(normalized,
                "list",
                "comma delimited list",
                "comma separated list",
                "in ascending order",
                "what page numbers",
                "which page numbers",
                "what are the",
                "which are the")) {
            return GaiaQuestionType.LIST_OR_SET;
        }
        if (matchesAny(normalized,
                "what species",
                "which species",
                "which bird",
                "which animal",
                "who is",
                "which company",
                "which source",
                "which city",
                "which country",
                "what is the name of",
                "what was the name of")) {
            return GaiaQuestionType.SINGLE_ENTITY;
        }
        return GaiaQuestionType.GENERAL;
    }

    private boolean matchesAny(String normalized, String... patterns) {
        for (String pattern : patterns) {
            if (normalized.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }
        return prompt.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
