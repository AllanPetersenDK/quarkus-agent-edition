package dk.ashlan.agent.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RagQueryAnalysis {
    private static final Pattern WHERE_MENTIONED = Pattern.compile("(?i)\\bwhere is\\s+(.+?)\\s+mentioned\\b");
    private static final Pattern WHAT_IS = Pattern.compile("(?i)\\bwhat is\\s+(.+?)(?:\\?|\\.|$)");
    private static final Pattern WHAT_ARE = Pattern.compile("(?i)\\bwhat are\\s+(.+?)(?:\\?|\\.|$)");
    private static final Pattern MENTIONS = Pattern.compile("(?i)\\bmentions?\\s+(.+?)(?:\\?|\\.|$)");

    private final String originalQuery;
    private final String normalizedQuery;
    private final String entityPhrase;
    private final String normalizedEntityPhrase;
    private final List<String> meaningfulTokens;
    private final boolean definitionQuery;
    private final boolean sourceAwareQuery;

    private RagQueryAnalysis(String originalQuery, String normalizedQuery, String entityPhrase, List<String> meaningfulTokens, boolean definitionQuery, boolean sourceAwareQuery) {
        this.originalQuery = originalQuery;
        this.normalizedQuery = normalizedQuery;
        this.entityPhrase = entityPhrase;
        this.normalizedEntityPhrase = RagTextUtils.normalize(entityPhrase);
        this.meaningfulTokens = List.copyOf(meaningfulTokens);
        this.definitionQuery = definitionQuery;
        this.sourceAwareQuery = sourceAwareQuery;
    }

    static RagQueryAnalysis analyze(String query) {
        String safeQuery = query == null ? "" : query.trim();
        String normalizedQuery = RagTextUtils.normalize(safeQuery);
        String entityPhrase = extractEntityPhrase(safeQuery);
        boolean definitionQuery = normalizedQuery.startsWith("what is ") || normalizedQuery.startsWith("what are ");
        boolean sourceAwareQuery = normalizedQuery.contains("which text mentions")
                || normalizedQuery.contains("which source mentions")
                || normalizedQuery.contains("which document mentions");
        return new RagQueryAnalysis(
                safeQuery,
                normalizedQuery,
                entityPhrase,
                meaningfulTokens(normalizedQuery),
                definitionQuery,
                sourceAwareQuery
        );
    }

    String originalQuery() {
        return originalQuery;
    }

    String normalizedQuery() {
        return normalizedQuery;
    }

    String entityPhrase() {
        return entityPhrase;
    }

    String normalizedEntityPhrase() {
        return normalizedEntityPhrase;
    }

    boolean hasEntityPhrase() {
        return !normalizedEntityPhrase.isBlank();
    }

    List<String> meaningfulTokens() {
        return meaningfulTokens;
    }

    boolean definitionQuery() {
        return definitionQuery;
    }

    boolean sourceAwareQuery() {
        return sourceAwareQuery;
    }

    boolean mentionsStyleQuery() {
        return normalizedQuery.contains("mention");
    }

    private static List<String> meaningfulTokens(String normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            return List.of();
        }
        String[] tokens = normalizedQuery.split(" ");
        List<String> result = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            if (token.isBlank() || STOP_WORDS.contains(token)) {
                continue;
            }
            result.add(token);
        }
        return result;
    }

    private static String extractEntityPhrase(String query) {
        for (Pattern pattern : List.of(WHERE_MENTIONED, WHAT_IS, WHAT_ARE, MENTIONS)) {
            Matcher matcher = pattern.matcher(query);
            if (matcher.find()) {
                return cleanupEntityPhrase(matcher.group(1));
            }
        }
        return "";
    }

    private static String cleanupEntityPhrase(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replaceAll("^[\\p{Punct}\\s]+", "")
                .replaceAll("[\\p{Punct}\\s]+$", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static final java.util.Set<String> STOP_WORDS = java.util.Set.of(
            "a", "an", "and", "are", "for", "from", "in", "is", "of", "on", "source", "text",
            "the", "to", "what", "where", "which", "who", "whom", "why", "with", "does", "do",
            "did", "mentions", "mention", "mentioned", "document"
    );
}
