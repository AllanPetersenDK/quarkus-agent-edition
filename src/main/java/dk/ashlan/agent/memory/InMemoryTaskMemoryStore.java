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
        if (normalizedQuery.isBlank() || limit <= 0) {
            return List.of();
        }
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
        double score = 0.0;
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

    private double tokenScore(String field, List<String> queryTokens, double weight) {
        if (field == null || field.isBlank() || queryTokens.isEmpty()) {
            return 0.0;
        }
        Set<String> fieldTokens = Set.copyOf(tokenize(field));
        if (fieldTokens.isEmpty()) {
            return 0.0;
        }
        long matches = queryTokens.stream()
                .filter(fieldTokens::contains)
                .count();
        return matches * weight;
    }

    private double phraseScore(String field, String normalizedQuery, double weight) {
        if (field == null || field.isBlank() || normalizedQuery.isBlank()) {
            return 0.0;
        }
        String normalizedField = normalize(field);
        if (normalizedField.isBlank()) {
            return 0.0;
        }
        if (normalizedField.equals(normalizedQuery)) {
            return weight * 1.5;
        }
        return normalizedField.contains(normalizedQuery) ? weight : 0.0;
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
