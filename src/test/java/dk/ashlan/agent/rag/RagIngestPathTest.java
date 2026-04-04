package dk.ashlan.agent.rag;

import dk.ashlan.agent.code.WorkspaceService;
import dk.ashlan.agent.document.DocumentReadService;
import dk.ashlan.agent.eval.gaia.GaiaAttachmentExtractionService;
import dk.ashlan.agent.eval.gaia.GaiaAudioTranscriptionService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagIngestPathTest {
    @TempDir
    Path tempDir;

    @Test
    void ingestPathReadsWorkspaceDocumentsAndChunksText() throws Exception {
        RagService ragService = ragService();
        writeFile("docs/sample.md", "# Chapter 5\nThe secret keyword is quarkus.");
        writeFile("docs/sample.csv", "name,value\nalpha,quarkus");
        writeFile("docs/sample.txt", "Plain text says quarkus.");
        writePdf("docs/sample.pdf", "PDF says quarkus.");

        assertIngested(ragService.ingestPath("docs/sample.md", null), "docs/sample.md", "quarkus");
        assertIngested(ragService.ingestPath("docs/sample.csv", null), "docs/sample.csv", "quarkus");
        assertIngested(ragService.ingestPath("docs/sample.txt", null), "docs/sample.txt", "quarkus");
        assertIngested(ragService.ingestPath("docs/sample.pdf", null), "docs/sample.pdf", "quarkus");
    }

    @Test
    void ingestPathRejectsDirectoryAndTraversalClearly() throws Exception {
        RagService ragService = ragService();
        Files.createDirectories(tempDir.resolve("workspace/folder"));

        RagService.RagPathIngestResult directory = ragService.ingestPath("folder", null);
        RagService.RagPathIngestResult traversal = ragService.ingestPath("../outside.txt", null);

        assertEquals("DIRECTORY_UNSUPPORTED", directory.status());
        assertEquals(0, directory.chunkCount());
        assertTrue(directory.error().contains("directory ingest/read is not supported yet"));

        assertEquals("ERROR", traversal.status());
        assertEquals(0, traversal.chunkCount());
        assertTrue(traversal.error().contains("Path traversal is not allowed") || traversal.error().contains("resolved path escapes workspace root"));
    }

    private RagService ragService() throws Exception {
        Path workspaceRoot = tempDir.resolve("workspace");
        WorkspaceService workspaceService = new WorkspaceService(workspaceRoot.toString());
        DocumentReadService documentReadService = new DocumentReadService(workspaceService, new GaiaAttachmentExtractionService(), audioTranscriptionService());
        Chunker chunker = new Chunker();
        EmbeddingClient embeddingClient = new ConstantEmbeddingClient();
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();
        DocumentIngestionService ingestionService = new DocumentIngestionService(chunker, embeddingClient, vectorStore);
        Retriever retriever = new Retriever(embeddingClient, vectorStore);
        return new RagService(ingestionService, retriever, documentReadService);
    }

    private void assertIngested(RagService.RagPathIngestResult result, String expectedPath, String expectedSnippet) {
        assertEquals("INGESTED", result.status());
        assertEquals(expectedPath, result.path());
        assertFalse(result.chunks().isEmpty());
        assertTrue(result.chunkCount() > 0);
        assertEquals(expectedPath, result.sourceId());
        assertTrue(result.traceEvents().contains("attachment:text-extracted") || result.traceEvents().contains("attachment:text-normalized") || result.traceEvents().contains("attachment:audio-transcribed"));
        assertTrue(result.chunks().stream().anyMatch(chunk -> chunk.text().contains(expectedSnippet)));
    }

    private void writeFile(String relativePath, String contents) throws Exception {
        Path file = tempDir.resolve("workspace").resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, contents);
    }

    private void writePdf(String relativePath, String text) throws Exception {
        Path file = tempDir.resolve("workspace").resolve(relativePath);
        Files.createDirectories(file.getParent());
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(72, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            document.save(file.toFile());
        }
    }

    private GaiaAudioTranscriptionService audioTranscriptionService() {
        return path -> "unused audio transcript";
    }

    private static final class ConstantEmbeddingClient implements EmbeddingClient {
        @Override
        public double[] embed(String text) {
            return new double[]{1.0, 1.0, 1.0};
        }
    }
}
