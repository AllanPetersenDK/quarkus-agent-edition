package dk.ashlan.agent.eval.gaia;

import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class GaiaAnswerScorer {
    public GaiaScoreResult score(List<String> expectedAnswers, String predictedAnswer) {
        String predictedNormalized = normalize(predictedAnswer);
        if (expectedAnswers == null || expectedAnswers.isEmpty()) {
            return new GaiaScoreResult(false, 0.0, "missing-expected-answer", "", predictedNormalized, null);
        }
        GaiaScoreResult best = null;
        for (String expected : expectedAnswers) {
            GaiaScoreResult candidate = scoreOne(expected, predictedAnswer, predictedNormalized);
            if (best == null || compare(candidate, best) > 0) {
                best = candidate;
            }
        }
        return best;
    }

    public GaiaScoreResult score(String expectedAnswer, String predictedAnswer) {
        return score(expectedAnswer == null ? List.of() : List.of(expectedAnswer), predictedAnswer);
    }

    public boolean matches(String expectedAnswer, String predictedAnswer) {
        return score(expectedAnswer, predictedAnswer).passed();
    }

    public String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.toLowerCase(Locale.ROOT).trim();
        normalized = normalized
                .replaceAll("(?i)^(final answer|answer|the answer is)\\s*[:\\-]?\\s*", "")
                .replaceAll("[\"'`]", "")
                .replaceAll("[,]", "")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\.\\-]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        normalized = stripLeadingArticle(normalized);
        normalized = normalizeSimplePlurals(normalized);
        return normalized;
    }

    private GaiaScoreResult scoreOne(String expectedAnswer, String predictedRaw, String predictedNormalized) {
        String expectedNormalized = normalize(expectedAnswer);
        BigDecimal expectedNumber = parseNumber(expectedNormalized);
        BigDecimal predictedNumber = parseNumber(predictedNormalized);
        if (expectedNumber != null && predictedNumber != null && expectedNumber.compareTo(predictedNumber) == 0) {
            return new GaiaScoreResult(true, 1.0, "numeric-equivalence", expectedNormalized, predictedNormalized, expectedAnswer);
        }
        if (expectedNormalized.equals(predictedNormalized)) {
            return new GaiaScoreResult(true, 1.0, "normalized-exact-match", expectedNormalized, predictedNormalized, expectedAnswer);
        }
        if (isShortAnswer(expectedNormalized) && phraseContains(predictedNormalized, expectedNormalized)) {
            return new GaiaScoreResult(true, 0.92, "short-answer-match", expectedNormalized, predictedNormalized, expectedAnswer);
        }
        if (phraseContains(predictedNormalized, expectedNormalized) || phraseContains(expectedNormalized, predictedNormalized)) {
            return new GaiaScoreResult(true, 0.80, "substring-fallback", expectedNormalized, predictedNormalized, expectedAnswer);
        }
        double tokenScore = tokenOverlapScore(expectedNormalized, predictedNormalized);
        boolean passed = tokenScore >= 0.88 && isShortAnswer(expectedNormalized);
        return new GaiaScoreResult(passed, tokenScore, passed ? "token-overlap-match" : "token-overlap-no-match", expectedNormalized, predictedNormalized, expectedAnswer);
    }

    private int compare(GaiaScoreResult left, GaiaScoreResult right) {
        int scoreCompare = Double.compare(left.score(), right.score());
        if (scoreCompare != 0) {
            return scoreCompare;
        }
        if (left.passed() != right.passed()) {
            return left.passed() ? 1 : -1;
        }
        return left.reason().compareTo(right.reason());
    }

    private boolean isShortAnswer(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.split("\\s+").length <= 6;
    }

    private boolean phraseContains(String haystack, String needle) {
        if (haystack == null || needle == null || haystack.isBlank() || needle.isBlank()) {
            return false;
        }
        String pattern = "\\b" + java.util.regex.Pattern.quote(needle) + "\\b";
        return java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE).matcher(haystack).find();
    }

    private double tokenOverlapScore(String expected, String predicted) {
        List<String> expectedTokens = tokens(expected);
        List<String> predictedTokens = tokens(predicted);
        if (expectedTokens.isEmpty() || predictedTokens.isEmpty()) {
            return 0.0;
        }
        long matches = expectedTokens.stream().filter(predictedTokens::contains).distinct().count();
        return (double) matches / (double) expectedTokens.size();
    }

    private List<String> tokens(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String[] parts = value.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            String cleaned = part.trim();
            if (!cleaned.isBlank()) {
                tokens.add(cleaned);
            }
        }
        return tokens;
    }

    private BigDecimal parseNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.replace("%", "").replace(",", "").trim();
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException exception) {
            return null;
        }
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

    private String normalizeSimplePlurals(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        List<String> tokens = new ArrayList<>();
        for (String token : value.split("\\s+")) {
            tokens.add(normalizePluralToken(token));
        }
        return String.join(" ", tokens).trim();
    }

    private String normalizePluralToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        String cleaned = token.trim();
        if (cleaned.length() > 4 && cleaned.endsWith("ies") && !cleaned.endsWith("eies")) {
            return cleaned.substring(0, cleaned.length() - 3) + "y";
        }
        if (cleaned.length() > 4 && cleaned.endsWith("es") && (
                cleaned.endsWith("ches")
                        || cleaned.endsWith("shes")
                        || cleaned.endsWith("xes")
                        || cleaned.endsWith("zes")
                        || cleaned.endsWith("ses"))) {
            return cleaned.substring(0, cleaned.length() - 2);
        }
        if (cleaned.length() > 4 && cleaned.endsWith("s")
                && !cleaned.endsWith("ss")
                && !cleaned.endsWith("us")
                && !cleaned.endsWith("is")
                && !cleaned.endsWith("ous")) {
            return cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }
}
