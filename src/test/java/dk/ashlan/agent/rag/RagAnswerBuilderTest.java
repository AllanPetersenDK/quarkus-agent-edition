package dk.ashlan.agent.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagAnswerBuilderTest {
    @Test
    void sourceAwareQueriesMentionTheBestSourceAndStayConcise() {
        RagAnswerBuilder builder = new RagAnswerBuilder();
        List<RetrievalResult> results = List.of(
                new RetrievalResult(
                        new DocumentChunk("chunk-1", "chapter5-test", 0, "PostgreSQL is an open-source relational database.", java.util.Map.of("source", "chapter5-test")),
                        0.91
                ),
                new RetrievalResult(
                        new DocumentChunk("chunk-2", "chapter5-test-2", 0, "H2 is an embedded database often used for local development.", java.util.Map.of("source", "chapter5-test-2")),
                        0.40
                )
        );

        String answer = builder.build("Which source mentions PostgreSQL?", results);

        assertTrue(answer.startsWith("Source chapter5-test mentions PostgreSQL: "));
        assertTrue(answer.endsWith("PostgreSQL is an open-source relational database."));
    }

    @Test
    void definitionQueriesUseASingleSentenceFromTheBestChunk() {
        RagAnswerBuilder builder = new RagAnswerBuilder();
        List<RetrievalResult> results = List.of(
                new RetrievalResult(
                        new DocumentChunk("chunk-1", "chapter5-test-2", 0, "LangChain4j is a Java library for building LLM-powered applications. H2 is an embedded database often used for local development.", java.util.Map.of("source", "chapter5-test-2")),
                        0.92
                )
        );

        String answer = builder.build("What is H2?", results);

        assertEquals("H2 is an embedded database often used for local development.", answer);
    }
}
