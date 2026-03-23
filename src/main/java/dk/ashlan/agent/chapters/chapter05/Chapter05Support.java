package dk.ashlan.agent.chapters.chapter05;

import dk.ashlan.agent.rag.Chunker;
import dk.ashlan.agent.rag.DocumentIngestionService;
import dk.ashlan.agent.rag.FakeEmbeddingClient;
import dk.ashlan.agent.rag.InMemoryVectorStore;
import dk.ashlan.agent.rag.KnowledgeBaseTool;
import dk.ashlan.agent.rag.RagService;
import dk.ashlan.agent.rag.Retriever;

final class Chapter05Support {
    private Chapter05Support() {
    }

    static RagService ragService() {
        return ragServiceStack().ragService();
    }

    static RagStack ragServiceStack() {
        Chunker chunker = new Chunker();
        FakeEmbeddingClient embeddingClient = new FakeEmbeddingClient();
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();
        DocumentIngestionService ingestionService = new DocumentIngestionService(chunker, embeddingClient, vectorStore);
        Retriever retriever = new Retriever(embeddingClient, vectorStore);
        return new RagStack(new RagService(ingestionService, retriever));
    }

    static KnowledgeBaseTool knowledgeBaseTool() {
        return new KnowledgeBaseTool(ragService());
    }

    static String ingestAndAnswer(String query) {
        RagService ragService = ragService();
        ragService.ingest("chapter-05", """
                Quarkus is a fast Java framework for cloud-native applications.

                The calculator tool evaluates arithmetic expressions.
                RAG lets an agent retrieve relevant knowledge before answering.
                """);
        return ragService.answer(query, 2);
    }

    static String knowledgeBaseAnswer(String query) {
        RagStack stack = ragServiceStack();
        stack.ragService().ingest("chapter-05", """
                Quarkus is a fast Java framework for cloud-native applications.

                The calculator tool evaluates arithmetic expressions.
                RAG lets an agent retrieve relevant knowledge before answering.
                """);
        return new KnowledgeBaseTool(stack.ragService())
                .execute(java.util.Map.of("query", query, "topK", 2))
                .output();
    }

    record RagStack(RagService ragService) {
    }
}
