package dk.ashlan.agent.core.callback;

import dk.ashlan.agent.core.AfterToolContext;
import dk.ashlan.agent.tools.JsonToolResult;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@ApplicationScoped
@Priority(100)
public class ToolResultCompressionCallback implements AgentCallback {
    private static final String WEB_SEARCH_TOOL = "web-search";
    private static final Set<String> COMPRESSIBLE_TOOLS = Set.of(
            "list_files",
            "read_file",
            "read_document_file",
            "read_media_file"
    );
    private static final int MAX_OUTPUT_CHARS = 1400;
    private static final int MAX_WEB_SEARCH_CHUNK_COUNT = 5;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern BLANK_LINES = Pattern.compile("(?:\\r?\\n){2,}");

    @Override
    public JsonToolResult afterTool(AfterToolContext context) {
        JsonToolResult result = context.toolResult();
        if (result == null || !result.success()) {
            return result;
        }
        String toolName = context.toolCall() == null ? result.toolName() : context.toolCall().toolName();
        if (WEB_SEARCH_TOOL.equals(toolName)) {
            return compressWebSearchResult(context, result, toolName);
        }
        if (!COMPRESSIBLE_TOOLS.contains(toolName)) {
            return result;
        }
        String output = result.output();
        if (output == null || output.length() <= MAX_OUTPUT_CHARS) {
            return result;
        }
        return truncateResult(result, toolName, output, "generic-truncation");
    }

    private JsonToolResult compressWebSearchResult(AfterToolContext context, JsonToolResult result, String toolName) {
        String output = result.output();
        if (output == null || output.length() <= MAX_OUTPUT_CHARS) {
            return result;
        }
        String query = extractQuery(context);
        if (query.isBlank()) {
            return truncateResult(result, toolName, output, "generic-truncation");
        }

        List<String> chunks = splitChunks(output);
        if (chunks.isEmpty()) {
            return truncateResult(result, toolName, output, "generic-truncation");
        }

        List<ScoredChunk> scoredChunks = scoreChunks(query, chunks);
        if (scoredChunks.isEmpty()) {
            return truncateResult(result, toolName, output, "generic-truncation");
        }

        int selectedChunkCount = scoredChunks.size() < 3 ? scoredChunks.size() : Math.min(MAX_WEB_SEARCH_CHUNK_COUNT, scoredChunks.size());
        List<ScoredChunk> selected = scoredChunks.stream()
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed()
                        .thenComparingInt(ScoredChunk::index))
                .limit(selectedChunkCount)
                .toList();

        String compressedOutput = buildSearchCompressionOutput(query, selected);
        if (compressedOutput.length() >= output.length()) {
            return truncateResult(result, toolName, output, "generic-truncation");
        }

        Map<String, Object> data = new LinkedHashMap<>(result.data());
        data.put("compressed", true);
        data.put("compressionStrategy", "search-query-ranking");
        data.put("originalLength", output.length());
        data.put("compressedLength", compressedOutput.length());
        data.put("selectedChunkCount", selected.size());
        data.put("query", query);
        return new JsonToolResult(toolName, true, compressedOutput, Map.copyOf(data));
    }

    private JsonToolResult truncateResult(JsonToolResult result, String toolName, String output, String strategy) {
        String trimmed = output.substring(0, MAX_OUTPUT_CHARS).stripTrailing();
        Map<String, Object> data = new LinkedHashMap<>(result.data());
        data.put("truncated", true);
        data.put("compressed", true);
        data.put("compressionStrategy", strategy);
        data.put("originalLength", output.length());
        data.put("compressedLength", trimmed.length());
        data.put("compressionNote", "Output truncated for callback compression.");
        String compressedOutput = trimmed + "\n...[truncated " + (output.length() - trimmed.length()) + " chars]";
        return new JsonToolResult(toolName, true, compressedOutput, Map.copyOf(data));
    }

    private String extractQuery(AfterToolContext context) {
        if (context == null || context.toolCall() == null || context.toolCall().arguments() == null) {
            return "";
        }
        Object query = context.toolCall().arguments().get("query");
        return query == null ? "" : String.valueOf(query).trim();
    }

    private List<String> splitChunks(String output) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = BLANK_LINES.split(output);
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.strip();
            if (!trimmed.isBlank()) {
                chunks.add(trimmed);
            }
        }
        if (chunks.size() > 1) {
            return chunks;
        }

        List<String> lines = output.lines()
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .toList();
        if (lines.size() > 1) {
            return lines;
        }

        if (output.contains(". ") || output.contains("? ") || output.contains("! ")) {
            String[] sentences = output.split("(?<=[.!?])\\s+");
            for (String sentence : sentences) {
                String trimmed = sentence.strip();
                if (!trimmed.isBlank()) {
                    chunks.add(trimmed);
                }
            }
        }
        return chunks.isEmpty() ? List.of(output.strip()) : chunks;
    }

    private List<ScoredChunk> scoreChunks(String query, List<String> chunks) {
        Set<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return List.of();
        }
        String normalizedQuery = normalize(query);
        List<ScoredChunk> scored = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            double score = scoreChunk(normalizedQuery, queryTokens, chunk);
            scored.add(new ScoredChunk(i, chunk, score));
        }
        return scored.stream()
                .filter(scoredChunk -> scoredChunk.score() > 0.0d || chunks.size() <= 3)
                .toList();
    }

    private double scoreChunk(String normalizedQuery, Set<String> queryTokens, String chunk) {
        String normalizedChunk = normalize(chunk);
        double score = 0.0d;
        if (normalizedChunk.contains(normalizedQuery) && !normalizedQuery.isBlank()) {
            score += 5.0d;
        }
        for (String token : queryTokens) {
            if (normalizedChunk.contains(token)) {
                score += 1.5d;
            }
        }
        score += sharedTokenBonus(queryTokens, normalizedChunk);
        score += phraseBoost(normalizedQuery, normalizedChunk);
        return score;
    }

    private double sharedTokenBonus(Set<String> queryTokens, String normalizedChunk) {
        double bonus = 0.0d;
        for (String token : queryTokens) {
            if (normalizedChunk.contains(token)) {
                bonus += 0.25d;
            }
        }
        return bonus;
    }

    private double phraseBoost(String normalizedQuery, String normalizedChunk) {
        if (normalizedQuery.isBlank() || normalizedChunk.isBlank()) {
            return 0.0d;
        }
        if (normalizedChunk.startsWith(normalizedQuery)) {
            return 1.0d;
        }
        if (normalizedChunk.contains(" " + normalizedQuery + " ")) {
            return 0.75d;
        }
        return 0.0d;
    }

    private String buildSearchCompressionOutput(String query, List<ScoredChunk> selected) {
        StringBuilder builder = new StringBuilder();
        builder.append("Search query: ").append(query).append('\n');
        builder.append("Selected results: ").append(selected.size()).append('\n');
        for (int i = 0; i < selected.size(); i++) {
            ScoredChunk chunk = selected.get(i);
            builder.append('\n')
                    .append('[').append(i + 1).append("] score=").append(String.format(java.util.Locale.ROOT, "%.2f", chunk.score()))
                    .append('\n')
                    .append(compactChunk(chunk.text()));
        }
        return builder.toString().stripTrailing();
    }

    private String compactChunk(String text) {
        String normalized = WHITESPACE.matcher(Objects.requireNonNullElse(text, "")).replaceAll(" ").strip();
        if (normalized.length() <= 500) {
            return normalized;
        }
        return normalized.substring(0, 500).stripTrailing() + "…";
    }

    private String normalize(String value) {
        return WHITESPACE.matcher(Objects.requireNonNullElse(value, "").toLowerCase(java.util.Locale.ROOT))
                .replaceAll(" ")
                .strip();
    }

    private Set<String> tokenize(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return Set.of();
        }
        Set<String> tokens = new HashSet<>();
        for (String token : normalized.split(" ")) {
            if (token.length() >= 3 && !isStopWord(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private boolean isStopWord(String token) {
        return STOP_WORDS.contains(token);
    }

    private record ScoredChunk(int index, String text, double score) {
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "from", "that", "this", "what", "which", "who", "are", "was", "were",
            "does", "did", "how", "many", "when", "where", "why", "whose", "into", "about", "your", "you",
            "can", "will", "could", "should", "would", "have", "has", "had", "not", "but", "use", "used", "useful",
            "search", "result", "results", "query", "live", "web"
    );
}
