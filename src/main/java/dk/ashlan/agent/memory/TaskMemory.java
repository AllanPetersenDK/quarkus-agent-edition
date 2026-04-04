package dk.ashlan.agent.memory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public record TaskMemory(
        String sessionId,
        String task,
        String memory,
        String taskSummary,
        String approach,
        String finalAnswer,
        Boolean correct,
        String errorAnalysis
) {
    private static final Set<String> TOKEN_STOPWORDS = Set.of(
            "a", "an", "and", "are", "be", "best", "for", "from", "got", "have", "i", "in", "is",
            "it", "like", "me", "my", "of", "on", "or", "please", "that", "the", "to", "up", "us",
            "we", "what", "when", "where", "which", "who", "why", "with", "you", "your"
    );

    public TaskMemory(String sessionId, String task, String memory) {
        this(sessionId, task, memory, null, null, null, null, null);
    }

    public String problem() {
        return isBlank(taskSummary) ? task : taskSummary;
    }

    public String summary() {
        return isBlank(taskSummary) ? problem() : taskSummary;
    }

    public String result() {
        return isBlank(finalAnswer) ? memory : finalAnswer;
    }

    public String searchableText() {
        return joinNonBlank(summary(), problem(), approach, result(), memory, errorAnalysis, task);
    }

    public String structuredDedupKey() {
        return normalizeForComparison(joinNonBlank(summary(), problem(), approach, result(), errorAnalysis, task, stripTrace(memory)));
    }

    public List<String> searchableTokens() {
        return tokenize(searchableText());
    }

    public List<String> dedupTokens() {
        return tokenize(structuredDedupKey());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String joinNonBlank(String... values) {
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" | "));
    }

    private String stripTrace(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        int traceIndex = normalized.toLowerCase(Locale.ROOT).indexOf(" | trace:");
        if (traceIndex >= 0) {
            normalized = normalized.substring(0, traceIndex).trim();
        }
        return normalized;
    }

    private String normalizeForComparison(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("\\blike\\s+best\\b", "favorite");
        normalized = normalized.replaceAll("\\bshort\\s+answers?\\b", "concise answers");
        normalized = normalized.replace("=>", " ");
        normalized = normalized.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ");
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private List<String> tokenize(String value) {
        String normalized = normalizeForComparison(value);
        if (normalized.isBlank()) {
            return List.of();
        }
        return Arrays.stream(normalized.split(" "))
                .filter(token -> token.length() >= 3)
                .filter(token -> !TOKEN_STOPWORDS.contains(token))
                .distinct()
                .toList();
    }
}
