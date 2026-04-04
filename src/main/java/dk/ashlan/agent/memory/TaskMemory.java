package dk.ashlan.agent.memory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
    public TaskMemory(String sessionId, String task, String memory) {
        this(sessionId, task, memory, null, null, null, null, null);
    }

    public String problem() {
        return isBlank(taskSummary) ? task : taskSummary;
    }

    public String result() {
        return isBlank(finalAnswer) ? memory : finalAnswer;
    }

    String searchableText() {
        return joinNonBlank(taskSummary, approach, finalAnswer, memory, errorAnalysis, task);
    }

    String dedupKey() {
        return normalizeForComparison(joinNonBlank(taskSummary, approach, finalAnswer, stripTrace(memory), errorAnalysis, task));
    }

    List<String> searchableTokens() {
        return tokenize(searchableText());
    }

    List<String> dedupTokens() {
        return tokenize(dedupKey());
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
                .distinct()
                .toList();
    }
}
