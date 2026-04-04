package dk.ashlan.agent.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class RagTextUtils {
    private RagTextUtils() {
    }

    static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    static List<String> tokenize(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return List.of();
        }
        String[] tokens = normalized.split(" ");
        List<String> result = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            if (!token.isBlank()) {
                result.add(token);
            }
        }
        return result;
    }

    static boolean containsNormalizedPhrase(String haystack, String phrase) {
        String normalizedHaystack = normalize(haystack);
        String normalizedPhrase = normalize(phrase);
        if (normalizedHaystack.isBlank() || normalizedPhrase.isBlank()) {
            return false;
        }
        return normalizedHaystack.contains(normalizedPhrase);
    }

    static List<String> sentences(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] split = text.replace('\n', ' ').split("(?<=[.!?])\\s+");
        List<String> result = new ArrayList<>(split.length);
        for (String sentence : split) {
            String cleaned = sentence.trim();
            if (!cleaned.isBlank()) {
                result.add(cleaned);
            }
        }
        if (result.isEmpty()) {
            return List.of(text.trim());
        }
        return result;
    }

    static String firstSentenceOrTrimmed(String text) {
        List<String> sentences = sentences(text);
        if (sentences.isEmpty()) {
            return "";
        }
        return sentences.get(0);
    }
}
