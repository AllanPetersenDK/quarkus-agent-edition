package dk.ashlan.agent.chapters.chapter05;

import dk.ashlan.agent.rag.RagService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class Chapter05PathIngestDemo {
    private final RagService ragService;

    @Inject
    public Chapter05PathIngestDemo(RagService ragService) {
        this.ragService = ragService;
    }

    public Chapter05PathIngestResult run(String question, String path, String sourceId) {
        String effectiveQuestion = question == null ? "" : question.trim();
        RagService.RagPathIngestResult ingestResult = ragService.ingestPath(path, sourceId);
        String answer = ingestResult.status().equals("INGESTED")
                ? ragService.answer(effectiveQuestion, 3)
                : "";
        return new Chapter05PathIngestResult(
                effectiveQuestion,
                path == null ? "" : path.trim(),
                ingestResult.sourceId(),
                ingestResult.status(),
                ingestResult.chunkCount(),
                answer,
                ingestResult.traceEvents()
        );
    }

    public record Chapter05PathIngestResult(
            String question,
            String path,
            String sourceId,
            String status,
            int chunkCount,
            String answer,
            List<String> trace
    ) {
    }
}
