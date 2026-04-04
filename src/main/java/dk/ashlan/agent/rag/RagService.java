package dk.ashlan.agent.rag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class RagService {
    private final DocumentIngestionService ingestionService;
    private final Retriever retriever;
    private final RagAnswerBuilder answerBuilder;

    @Inject
    public RagService(DocumentIngestionService ingestionService, Retriever retriever, RagAnswerBuilder answerBuilder) {
        this.ingestionService = ingestionService;
        this.retriever = retriever;
        this.answerBuilder = answerBuilder;
    }

    public RagService(DocumentIngestionService ingestionService, Retriever retriever) {
        this(ingestionService, retriever, new RagAnswerBuilder());
    }

    public List<DocumentChunk> ingest(String sourceId, String text) {
        return ingestionService.ingest(sourceId, text);
    }

    public List<RetrievalResult> retrieve(String query, int topK) {
        return retriever.retrieve(query, topK);
    }

    public String answer(String query, int topK) {
        return answer(query, retrieve(query, topK));
    }

    public String answer(String query, List<RetrievalResult> results) {
        return answerBuilder.build(query, results);
    }
}
