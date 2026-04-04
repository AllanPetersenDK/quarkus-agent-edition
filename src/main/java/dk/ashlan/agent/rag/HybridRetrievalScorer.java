package dk.ashlan.agent.rag;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.List;

@ApplicationScoped
final class HybridRetrievalScorer {
    List<RetrievalResult> rerank(String query, List<RetrievalResult> candidates, int topK) {
        if (candidates.isEmpty() || topK <= 0) {
            return List.of();
        }
        RagQueryAnalysis analysis = RagQueryAnalysis.analyze(query);
        Comparator<ScoredRetrievalResult> comparator = Comparator
                .comparingDouble(ScoredRetrievalResult::score).reversed()
                .thenComparing(Comparator.comparingDouble((ScoredRetrievalResult scored) -> scored.result().similarity()).reversed())
                .thenComparing(scored -> scored.result().chunk().sourceId())
                .thenComparingInt(scored -> scored.result().chunk().chunkIndex());
        return candidates.stream()
                .map(result -> new ScoredRetrievalResult(result, score(analysis, result)))
                .sorted(comparator)
                .limit(topK)
                .map(ScoredRetrievalResult::result)
                .toList();
    }

    double score(String query, RetrievalResult result) {
        return score(RagQueryAnalysis.analyze(query), result);
    }

    private double score(RagQueryAnalysis analysis, RetrievalResult result) {
        double cosineScore = clamp(result.similarity());
        double lexicalScore = lexicalOverlap(analysis.meaningfulTokens(), RagTextUtils.tokenize(result.chunk().text()));
        double entityScore = entityMatchScore(analysis, result.chunk().text());
        double phraseScore = RagTextUtils.containsNormalizedPhrase(result.chunk().text(), analysis.normalizedQuery()) ? 0.8 : 0.0;
        double intentBonus = 0.0;
        if (analysis.sourceAwareQuery() && entityScore > 0.0) {
            intentBonus += 0.05;
        }
        if (analysis.definitionQuery() && entityScore > 0.0) {
            intentBonus += 0.03;
        }
        if (analysis.mentionsStyleQuery() && entityScore > 0.0) {
            intentBonus += 0.02;
        }
        return cosineScore * 0.10 + lexicalScore * 0.10 + entityScore * 0.20 + phraseScore + intentBonus;
    }

    private double lexicalOverlap(List<String> queryTokens, List<String> chunkTokens) {
        if (queryTokens.isEmpty() || chunkTokens.isEmpty()) {
            return 0.0;
        }
        long matches = queryTokens.stream().distinct().filter(chunkTokens::contains).count();
        return (double) matches / (double) queryTokens.stream().distinct().count();
    }

    private double entityMatchScore(RagQueryAnalysis analysis, String chunkText) {
        if (!analysis.hasEntityPhrase()) {
            return 0.0;
        }
        if (RagTextUtils.containsNormalizedPhrase(chunkText, analysis.entityPhrase())) {
            return 1.0;
        }
        List<String> entityTokens = RagTextUtils.tokenize(analysis.entityPhrase());
        if (entityTokens.isEmpty()) {
            return 0.0;
        }
        List<String> chunkTokens = RagTextUtils.tokenize(chunkText);
        long matches = entityTokens.stream().distinct().filter(chunkTokens::contains).count();
        if (matches == entityTokens.stream().distinct().count()) {
            return 0.75;
        }
        if (matches > 0) {
            return 0.25;
        }
        return 0.0;
    }

    private double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private record ScoredRetrievalResult(RetrievalResult result, double score) {
    }
}
