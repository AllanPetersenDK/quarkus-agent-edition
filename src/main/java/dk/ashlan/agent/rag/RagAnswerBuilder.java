package dk.ashlan.agent.rag;

import java.util.List;
import java.util.Optional;

final class RagAnswerBuilder {
    String build(String query, List<RetrievalResult> results) {
        if (results.isEmpty()) {
            return "No relevant knowledge found.";
        }
        RagQueryAnalysis analysis = RagQueryAnalysis.analyze(query);
        RetrievalResult best = results.get(0);
        String snippet = snippet(best.chunk().text(), analysis);
        if (analysis.sourceAwareQuery() && analysis.hasEntityPhrase()) {
            return "Source " + best.chunk().sourceId() + " mentions " + analysis.entityPhrase() + ": " + snippet;
        }
        return snippet;
    }

    private String snippet(String chunkText, RagQueryAnalysis analysis) {
        if (chunkText == null || chunkText.isBlank()) {
            return "";
        }
        if (analysis.hasEntityPhrase()) {
            String normalizedEntity = analysis.normalizedEntityPhrase();
            Optional<String> matchingSentence = RagTextUtils.sentences(chunkText).stream()
                    .filter(sentence -> RagTextUtils.normalize(sentence).contains(normalizedEntity))
                    .findFirst();
            if (matchingSentence.isPresent()) {
                return matchingSentence.get();
            }
        }
        return RagTextUtils.firstSentenceOrTrimmed(chunkText);
    }
}
