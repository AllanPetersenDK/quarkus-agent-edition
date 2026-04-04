package dk.ashlan.agent.memory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

final class TaskMemoryRanking {
    private static final Set<String> STOP_WORDS = Set.of(
            "the",
            "and",
            "for",
            "with",
            "that",
            "this",
            "from",
            "what",
            "your",
            "into",
            "about",
            "have",
            "will",
            "when",
            "where",
            "which"
    );
    private static final int VECTOR_DIMENSION = 32;

    private TaskMemoryRanking() {
    }

    static double score(TaskMemory memory, String query) {
        if (memory == null) {
            return 0.0d;
        }
        String normalizedQuery = normalize(query);
        List<String> queryTokens = tokenize(query);
        if (normalizedQuery.isBlank()) {
            return 0.0d;
        }

        double score = 0.0d;
        score += phraseScore(memory.summary(), normalizedQuery, 8.0);
        score += phraseScore(memory.problem(), normalizedQuery, 7.0);
        score += phraseScore(memory.approach(), normalizedQuery, 4.0);
        score += phraseScore(memory.result(), normalizedQuery, 6.0);
        score += phraseScore(memory.memory(), normalizedQuery, 3.0);
        score += phraseScore(memory.errorAnalysis(), normalizedQuery, 1.5);
        score += phraseScore(memory.task(), normalizedQuery, 1.0);

        score += tokenScore(memory.summary(), queryTokens, 4.5);
        score += tokenScore(memory.problem(), queryTokens, 4.0);
        score += tokenScore(memory.approach(), queryTokens, 2.5);
        score += tokenScore(memory.result(), queryTokens, 4.0);
        score += tokenScore(memory.memory(), queryTokens, 2.0);
        score += tokenScore(memory.errorAnalysis(), queryTokens, 1.0);
        score += tokenScore(memory.task(), queryTokens, 1.5);
        score += overlapBoost(memory.searchableTokens(), queryTokens);
        if (normalize(memory.structuredDedupKey()).contains(normalizedQuery)) {
            score += 1.0;
        }
        if (normalize(memory.result()).contains(normalizedQuery) && !normalize(memory.summary()).contains(normalizedQuery)) {
            score += 0.5;
        }
        return score;
    }

    static double[] vectorize(String value) {
        double[] vector = new double[VECTOR_DIMENSION];
        for (String token : tokenize(value)) {
            int bucket = Math.floorMod(token.hashCode(), VECTOR_DIMENSION);
            vector[bucket] += 1.0d;
        }
        return vector;
    }

    static double cosineSimilarity(double[] left, double[] right) {
        if (left == null || right == null || left.length == 0 || right.length == 0) {
            return 0.0d;
        }
        double dot = 0.0d;
        double leftMagnitude = 0.0d;
        double rightMagnitude = 0.0d;
        for (int index = 0; index < Math.min(left.length, right.length); index++) {
            dot += left[index] * right[index];
            leftMagnitude += left[index] * left[index];
            rightMagnitude += right[index] * right[index];
        }
        if (leftMagnitude == 0.0d || rightMagnitude == 0.0d) {
            return 0.0d;
        }
        return dot / (Math.sqrt(leftMagnitude) * Math.sqrt(rightMagnitude));
    }

    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace("=>", " ")
                .replaceAll("(?i)\\s*\\|\\s*trace:.*$", " ")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    static List<String> tokenize(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return List.of();
        }
        return Arrays.stream(normalized.split(" "))
                .filter(token -> token.length() >= 3 && !STOP_WORDS.contains(token))
                .distinct()
                .collect(Collectors.toList());
    }

    private static double phraseScore(String field, String normalizedQuery, double weight) {
        if (field == null || field.isBlank() || normalizedQuery.isBlank()) {
            return 0.0d;
        }
        String normalizedField = normalize(field);
        if (normalizedField.isBlank()) {
            return 0.0d;
        }
        if (normalizedField.equals(normalizedQuery)) {
            return weight * 1.5d;
        }
        return normalizedField.contains(normalizedQuery) ? weight : 0.0d;
    }

    private static double tokenScore(String field, List<String> queryTokens, double weight) {
        if (field == null || field.isBlank() || queryTokens.isEmpty()) {
            return 0.0d;
        }
        Set<String> fieldTokens = Set.copyOf(tokenize(field));
        if (fieldTokens.isEmpty()) {
            return 0.0d;
        }
        long matches = queryTokens.stream().filter(fieldTokens::contains).count();
        return matches * weight;
    }

    private static double overlapBoost(List<String> memoryTokens, List<String> queryTokens) {
        if (memoryTokens.isEmpty() || queryTokens.isEmpty()) {
            return 0.0d;
        }
        long matches = queryTokens.stream().filter(memoryTokens::contains).count();
        return ((double) matches / (double) queryTokens.size()) * 2.5d;
    }
}
