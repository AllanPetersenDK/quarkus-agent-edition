package dk.ashlan.agent.rag;

import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class FakeEmbeddingClient implements EmbeddingClient {
    private static final int DIMENSIONS = 16;

    @Override
    public double[] embed(String text) {
        double[] vector = new double[DIMENSIONS];
        if (text == null) {
            return vector;
        }
        byte[] bytes = text.toLowerCase().getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            int index = i % DIMENSIONS;
            vector[index] += (bytes[i] & 0xFF) / 255.0;
        }
        return vector;
    }
}
