package dk.ashlan.agent.rag;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class InMemoryVectorStore implements VectorStore {
    private record StoredVector(DocumentChunk chunk, double[] vector) {
    }

    private final List<StoredVector> vectors = new ArrayList<>();

    @Override
    public void add(DocumentChunk chunk, double[] vector) {
        vectors.add(new StoredVector(chunk, vector.clone()));
    }

    @Override
    public List<RetrievalResult> search(double[] vector, int topK) {
        return vectors.stream()
                .map(stored -> new RetrievalResult(stored.chunk(), cosineSimilarity(vector, stored.vector())))
                .sorted(Comparator.comparingDouble(RetrievalResult::similarity).reversed())
                .limit(topK)
                .toList();
    }

    private double cosineSimilarity(double[] left, double[] right) {
        double dot = 0.0;
        double leftMagnitude = 0.0;
        double rightMagnitude = 0.0;
        for (int i = 0; i < Math.min(left.length, right.length); i++) {
            dot += left[i] * right[i];
            leftMagnitude += left[i] * left[i];
            rightMagnitude += right[i] * right[i];
        }
        if (leftMagnitude == 0 || rightMagnitude == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftMagnitude) * Math.sqrt(rightMagnitude));
    }
}
