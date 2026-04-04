package dk.ashlan.agent;

import dk.ashlan.agent.rag.Chunker;
import dk.ashlan.agent.rag.DocumentIngestionService;
import dk.ashlan.agent.rag.DocumentChunk;
import dk.ashlan.agent.rag.EmbeddingClient;
import dk.ashlan.agent.rag.FakeEmbeddingClient;
import dk.ashlan.agent.rag.InMemoryVectorStore;
import dk.ashlan.agent.rag.RagService;
import dk.ashlan.agent.rag.Retriever;
import dk.ashlan.agent.rag.RetrievalResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagServiceTest {
    @Test
    void entityMatchQueryRanksThePostgreSqlChunkFirst() {
        RagService ragService = ragServiceWithConstantEmbeddings();
        ragService.ingest("chapter5-test", """
                PostgreSQL is an open-source relational database. Quarkus is a Java framework optimized for cloud-native applications.
                """);
        ragService.ingest("chapter5-test-2", """
                LangChain4j is a Java library for building LLM-powered applications. H2 is an embedded database often used for local development.
                """);

        List<DocumentChunk> chunks = ragService.ingest("chapter5-test-3", """
                A completely unrelated chunk about something else.
                """);

        assertFalse(chunks.isEmpty());

        RetrievalResult result = ragService.retrieve("Which text mentions PostgreSQL?", 1).get(0);
        assertEquals("chapter5-test", result.chunk().sourceId());
        assertEquals("chapter5-test", result.chunk().metadata().get("sourceId"));
        assertEquals("chapter5-test", result.chunk().metadata().get("source"));
        assertEquals("0", result.chunk().metadata().get("chunkIndex"));
    }

    @Test
    void answerUsesBestChunkAndIsSourceAwareForMentionQueries() {
        RagService ragService = ragServiceWithConstantEmbeddings();
        ragService.ingest("chapter5-test", """
                PostgreSQL is an open-source relational database. Quarkus is a Java framework optimized for cloud-native applications.
                """);
        ragService.ingest("chapter5-test-2", """
                LangChain4j is a Java library for building LLM-powered applications. H2 is an embedded database often used for local development.
                """);

        String answer = ragService.answer("Which text mentions PostgreSQL?", 2);

        assertTrue(answer.contains("chapter5-test"));
        assertTrue(answer.contains("PostgreSQL is an open-source relational database."));
        assertFalse(answer.contains("H2 is an embedded database"));
    }

    @Test
    void definitionQueryReturnsAConciseAnswerFromTheBestChunk() {
        RagService ragService = ragServiceWithConstantEmbeddings();
        ragService.ingest("chapter5-test", """
                PostgreSQL is an open-source relational database. Quarkus is a Java framework optimized for cloud-native applications.
                """);
        ragService.ingest("chapter5-test-2", """
                LangChain4j is a Java library for building LLM-powered applications. H2 is an embedded database often used for local development.
                """);

        String answer = ragService.answer("What is H2?", 2);

        assertTrue(answer.startsWith("H2 is an embedded database often used for local development."));
        assertFalse(answer.contains("PostgreSQL"));
        assertFalse(answer.contains("\n"));
    }

    @Test
    void generalQueryStillWorks() {
        RagService ragService = ragServiceWithConstantEmbeddings();
        ragService.ingest("chapter5-test", """
                PostgreSQL is an open-source relational database. Quarkus is a Java framework optimized for cloud-native applications.
                """);
        ragService.ingest("chapter5-test-2", """
                LangChain4j is a Java library for building LLM-powered applications. H2 is an embedded database often used for local development.
                """);

        String answer = ragService.answer("What is Quarkus?", 2);

        assertTrue(answer.contains("Quarkus is a Java framework optimized for cloud-native applications."));
    }

    private RagService ragServiceWithConstantEmbeddings() {
        Chunker chunker = new Chunker();
        EmbeddingClient embeddingClient = new ConstantEmbeddingClient();
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();
        DocumentIngestionService ingestionService = new DocumentIngestionService(chunker, embeddingClient, vectorStore);
        Retriever retriever = new Retriever(embeddingClient, vectorStore);
        return new RagService(ingestionService, retriever);
    }

    private static final class ConstantEmbeddingClient implements EmbeddingClient {
        @Override
        public double[] embed(String text) {
            return new double[]{1.0, 1.0, 1.0};
        }
    }
}
