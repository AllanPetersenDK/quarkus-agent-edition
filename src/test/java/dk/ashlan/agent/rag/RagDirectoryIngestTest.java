package dk.ashlan.agent.rag;

import dk.ashlan.agent.code.WorkspaceService;
import dk.ashlan.agent.document.DocumentReadService;
import dk.ashlan.agent.eval.gaia.GaiaAttachmentExtractionService;
import dk.ashlan.agent.eval.gaia.GaiaAudioTranscriptionService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Rectangle;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagDirectoryIngestTest {
    @TempDir
    Path tempDir;

    @Test
    void directoryIngestProcessesSupportedFilesAndSkipsUnsupportedTopLevelEntries() throws Exception {
        RagService ragService = ragService();
        writeFile("docs/a.txt", "Alpha document mentions quarkus.");
        writeFile("docs/b.md", "Beta markdown mentions h2.");
        writeFile("docs/c.bin", "binary-noise");
        writeFile("docs/nested/nested.txt", "Nested document mentions postgres.");

        RagDirectoryIngestResult result = ragService.ingestDirectory("docs", "samples", false, 20);

        assertEquals("docs", result.path());
        assertEquals(4, result.totalCandidates());
        assertEquals(2, result.ingestedCount());
        assertTrue(result.skippedCount() >= 2);
        assertEquals(0, result.failedCount());
        assertTrue(result.results().stream().anyMatch(item -> "INGESTED".equals(item.status())));
        assertTrue(result.results().stream().anyMatch(item -> "SKIPPED_UNSUPPORTED".equals(item.status())));
        assertTrue(result.results().stream().anyMatch(item -> "SKIPPED_DIRECTORY".equals(item.status())));
        assertTrue(result.results().stream().filter(item -> "INGESTED".equals(item.status())).allMatch(item -> item.sourceId().startsWith("samples/docs/")));
    }

    @Test
    void recursiveDirectoryIngestIncludesNestedFilesAndRespectsLimit() throws Exception {
        RagService ragService = ragService();
        writeFile("docs/a.txt", "Alpha document mentions quarkus.");
        writeFile("docs/b.md", "Beta markdown mentions h2.");
        writeFile("docs/nested/nested.txt", "Nested document mentions postgres.");

        RagDirectoryIngestResult recursive = ragService.ingestDirectory("docs", null, true, 20);
        RagDirectoryIngestResult limited = ragService.ingestDirectory("docs", null, true, 2);

        assertTrue(recursive.results().stream().anyMatch(item -> item.path().equals("docs/nested/nested.txt")));
        assertTrue(recursive.results().stream().filter(item -> "INGESTED".equals(item.status())).anyMatch(item -> item.sourceId().equals("docs/nested/nested.txt")));
        assertEquals(2, limited.totalCandidates());
        assertEquals(2, limited.results().size());
    }

    @Test
    void directoryIngestProcessesOfficeAndNotebookDocuments() throws Exception {
        RagService ragService = ragService();
        writeDocx("docs/sample.docx", "DOCX mentions quarkus.");
        writePptx("docs/sample.pptx", "PPTX mentions h2.");
        writeXlsx("docs/sample.xlsx", "XLSX mentions postgres.");
        writeNotebook("docs/sample.ipynb", "IPYNB mentions langchain4j.");

        RagDirectoryIngestResult result = ragService.ingestDirectory("docs", null, false, 20);

        assertEquals(4, result.ingestedCount());
        assertTrue(result.results().stream().allMatch(item -> "INGESTED".equals(item.status())));
        assertTrue(result.results().stream().allMatch(item -> item.sourceId().startsWith("docs/")));
    }

    @Test
    void directoryIngestRejectsTraversalClearly() throws Exception {
        RagService ragService = ragService();

        RagDirectoryIngestResult result = ragService.ingestDirectory("../outside", null, false, 20);

        assertEquals(0, result.totalCandidates());
        assertFalse(result.error().isBlank());
        assertTrue(result.error().contains("Path traversal is not allowed") || result.error().contains("resolved path escapes workspace root"));
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

    private void writeFile(String relativePath, String contents) throws Exception {
        Path file = tempDir.resolve("workspace").resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, contents);
    }

    private void writeDocx(String relativePath, String text) throws Exception {
        Path file = tempDir.resolve("workspace").resolve(relativePath);
        Files.createDirectories(file.getParent());
        try (XWPFDocument document = new XWPFDocument()) {
            document.createParagraph().createRun().setText(text);
            try (FileOutputStream output = new FileOutputStream(file.toFile())) {
                document.write(output);
            }
        }
    }

    private void writePptx(String relativePath, String text) throws Exception {
        Path file = tempDir.resolve("workspace").resolve(relativePath);
        Files.createDirectories(file.getParent());
        try (XMLSlideShow slideshow = new XMLSlideShow()) {
            XSLFSlide slide = slideshow.createSlide();
            XSLFTextBox textBox = slide.createTextBox();
            textBox.setAnchor(new Rectangle(50, 50, 400, 100));
            textBox.setText(text);
            try (FileOutputStream output = new FileOutputStream(file.toFile())) {
                slideshow.write(output);
            }
        }
    }

    private void writeXlsx(String relativePath, String text) throws Exception {
        Path file = tempDir.resolve("workspace").resolve(relativePath);
        Files.createDirectories(file.getParent());
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Row row = workbook.createSheet("Sheet1").createRow(0);
            row.createCell(0).setCellValue(text);
            try (FileOutputStream output = new FileOutputStream(file.toFile())) {
                workbook.write(output);
            }
        }
    }

    private void writeNotebook(String relativePath, String text) throws Exception {
        Path file = tempDir.resolve("workspace").resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                {
                  "cells": [
                    {
                      "cell_type": "markdown",
                      "source": ["%s"],
                      "metadata": {}
                    }
                  ],
                  "metadata": {},
                  "nbformat": 4,
                  "nbformat_minor": 5
                }
                """.formatted(text));
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
