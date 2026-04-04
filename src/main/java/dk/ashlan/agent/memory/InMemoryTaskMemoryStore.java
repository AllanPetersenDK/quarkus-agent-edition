package dk.ashlan.agent.memory;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@ApplicationScoped
public class InMemoryTaskMemoryStore implements TaskMemoryStore {
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
    private final List<TaskMemory> memories = new CopyOnWriteArrayList<>();

    @Override
    public void save(TaskMemory taskMemory) {
        memories.add(taskMemory);
    }

    @Override
    public List<TaskMemory> findRelevant(String sessionId, String query, int limit) {
        String normalizedQuery = normalize(query);
        List<String> queryTokens = tokenize(query);
        return memories.stream()
                .sorted(Comparator
                        .comparingDouble((TaskMemory memory) -> score(memory, normalizedQuery, queryTokens))
                        .reversed())
                .limit(limit)
                .toList();
    }

    private double score(TaskMemory memory, String normalizedQuery, List<String> queryTokens) {
        if (memory == null || normalizedQuery.isBlank()) {
            return 0.0;
        }
        String searchable = normalize(memory.searchableText());
        String dedup = normalize(memory.dedupKey());
        double score = 0.0;
        if (searchable.contains(normalizedQuery)) {
            score += 8.0;
        }
        if (!normalize(memory.problem()).isBlank() && normalize(memory.problem()).contains(normalizedQuery)) {
            score += 3.0;
        }
        if (!normalize(memory.result()).isBlank() && normalize(memory.result()).contains(normalizedQuery)) {
            score += 3.0;
        }
        score += tokenScore(memory.taskSummary(), queryTokens, 4.0);
        score += tokenScore(memory.approach(), queryTokens, 2.0);
        score += tokenScore(memory.finalAnswer(), queryTokens, 3.0);
        score += tokenScore(memory.memory(), queryTokens, 2.5);
        score += tokenScore(memory.errorAnalysis(), queryTokens, 1.0);
        score += tokenScore(memory.task(), queryTokens, 1.5);
        score += overlapBoost(memory.searchableTokens(), queryTokens);
        if (dedup.contains(normalizedQuery)) {
            score += 1.5;
        }
        return score;
    }

    private double tokenScore(String field, List<String> queryTokens, double weight) {
        if (field == null || field.isBlank() || queryTokens.isEmpty()) {
            return 0.0;
        }
        String normalizedField = normalize(field);
        long matches = queryTokens.stream()
                .filter(token -> normalizedField.contains(token))
                .count();
        return matches * weight;
    }

    private double overlapBoost(List<String> memoryTokens, List<String> queryTokens) {
        if (memoryTokens.isEmpty() || queryTokens.isEmpty()) {
            return 0.0;
        }
        long matches = queryTokens.stream().filter(memoryTokens::contains).count();
        return ((double) matches / (double) queryTokens.size()) * 2.5;
    }

    private String normalize(String value) {
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

    private List<String> tokenize(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return List.of();
        }
        return Arrays.stream(normalized.split(" "))
                .filter(token -> token.length() >= 3 && !STOP_WORDS.contains(token))
                .distinct()
                .collect(Collectors.toList());
    }
}
