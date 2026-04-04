package dk.ashlan.agent.eval.gaia;

import java.util.Locale;

final class GaiaExactMatchScorer {
    boolean matches(String expected, String predicted) {
        return normalize(expected).equals(normalize(predicted));
    }

    String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.toLowerCase(Locale.ROOT).trim();
        normalized = normalized
                .replaceAll("(?i)^(final answer|answer)\\s*[:\\-]\\s*", "")
                .replaceAll("(?i)^the answer is\\s+", "")
                .replaceAll("[\"'`]", "")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        normalized = stripLeadingArticle(normalized);
        return normalized;
    }

    private String stripLeadingArticle(String value) {
        if (value.startsWith("the ")) {
            return value.substring(4).trim();
        }
        if (value.startsWith("a ")) {
            return value.substring(2).trim();
        }
        if (value.startsWith("an ")) {
            return value.substring(3).trim();
        }
        return value;
    }
}
