package dk.ashlan.agent.rag;

import java.util.List;

public interface VectorStore {
    void add(DocumentChunk chunk, double[] vector);

    List<RetrievalResult> search(double[] vector, int topK);
}
