package dk.ashlan.agent.rag;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class Chunker {
    public List<DocumentChunk> chunk(String sourceId, String text, int maxChars) {
        List<DocumentChunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        String[] paragraphs = text.split("\\n\\s*\\n");
        int index = 0;
        for (String paragraph : paragraphs) {
            String cleaned = paragraph.trim();
            if (cleaned.isBlank()) {
                continue;
            }
            for (int start = 0; start < cleaned.length(); start += maxChars) {
                int end = Math.min(cleaned.length(), start + maxChars);
                String chunkText = cleaned.substring(start, end).trim();
                if (!chunkText.isBlank()) {
                    chunks.add(new DocumentChunk(sourceId + "-" + index, sourceId, index, chunkText, Map.of("source", sourceId)));
                    index++;
                }
            }
        }
        return chunks;
    }
}
