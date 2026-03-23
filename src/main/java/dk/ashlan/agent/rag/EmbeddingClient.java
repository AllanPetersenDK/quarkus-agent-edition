package dk.ashlan.agent.rag;

public interface EmbeddingClient {
    double[] embed(String text);
}
