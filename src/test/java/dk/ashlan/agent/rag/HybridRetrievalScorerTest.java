package dk.ashlan.agent.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HybridRetrievalScorerTest {
    @Test
    void entityMatchBoostsTheRelevantChunkAboveAHigherCosineIrrelevantChunk() {
        HybridRetrievalScorer scorer = new HybridRetrievalScorer();
        RetrievalResult irrelevant = new RetrievalResult(
                new DocumentChunk("chunk-1", "chapter5-test-2", 0, "LangChain4j is a Java library for building LLM-powered applications.", java.util.Map.of("source", "chapter5-test-2")),
                0.95
        );
        RetrievalResult relevant = new RetrievalResult(
                new DocumentChunk("chunk-2", "chapter5-test", 0, "PostgreSQL is an open-source relational database.", java.util.Map.of("source", "chapter5-test")),
                0.10
        );

        List<RetrievalResult> reranked = scorer.rerank("Which text mentions PostgreSQL?", List.of(irrelevant, relevant), 1);

        assertEquals("chapter5-test", reranked.get(0).chunk().sourceId());
    }
}
