package dk.ashlan.agent.rag;

public record RetrievalResult(DocumentChunk chunk, double similarity) {
}
