package dk.ashlan.agent.rag;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkerTest {
    @Test
    void chunkerAppliesOverlapAndKeepsStableIds() {
        Chunker chunker = new Chunker();
        String text = """
                Alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu nu xi omicron pi rho sigma tau upsilon phi chi psi omega.

                Second paragraph stays separate and still proves the natural boundary behavior.
                """;

        List<DocumentChunk> chunks = chunker.chunk(
                "docs/sample.txt",
                text,
                80,
                Map.of(
                        "sourcePath", "docs/sample.txt",
                        "fileType", "txt",
                        "contentType", "text/plain",
                        "documentStatus", "TEXT_EXTRACTED",
                        "documentName", "sample.txt"
                )
        );
        List<DocumentChunk> repeated = chunker.chunk(
                "docs/sample.txt",
                text,
                80,
                Map.of(
                        "sourcePath", "docs/sample.txt",
                        "fileType", "txt",
                        "contentType", "text/plain",
                        "documentStatus", "TEXT_EXTRACTED",
                        "documentName", "sample.txt"
                )
        );

        assertTrue(chunks.size() > 1);
        assertEquals(chunks.get(0).id(), repeated.get(0).id());
        assertEquals(chunks.get(0).metadata().get("chunkIndex"), "0");
        assertEquals("docs/sample.txt", chunks.get(0).metadata().get("sourcePath"));
        assertEquals("txt", chunks.get(0).metadata().get("fileType"));
        assertEquals("TEXT_EXTRACTED", chunks.get(0).metadata().get("documentStatus"));
        assertFalse(chunks.get(1).text().isBlank());
        String overlap = chunks.get(0).text().substring(Math.max(0, chunks.get(0).text().length() - 8));
        assertTrue(chunks.get(1).text().contains(overlap));
    }
}
