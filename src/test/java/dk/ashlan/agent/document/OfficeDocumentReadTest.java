package dk.ashlan.agent.document;

import dk.ashlan.agent.code.WorkspaceService;
import dk.ashlan.agent.eval.gaia.GaiaAttachmentExtractionService;
import dk.ashlan.agent.eval.gaia.GaiaAudioTranscriptionService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Rectangle;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficeDocumentReadTest {
    @TempDir
    Path tempDir;

    @Test
    void readsDocxPptxXlsxAndIpynbThroughSharedDocumentReadLayer() throws Exception {
        DocumentReadService service = service();
        writeDocx("docs/sample.docx", "Hello from DOCX");
        writePptx("docs/sample.pptx", "Hello from PPTX");
        writeXlsx("docs/sample.xlsx", "Hello from XLSX");
        writeNotebook("docs/sample.ipynb", "Hello from IPYNB");

        assertContains(service.readDocumentFile("docs/sample.docx"), "Hello from DOCX");
        assertContains(service.readDocumentFile("docs/sample.pptx"), "Hello from PPTX");
        assertContains(service.readDocumentFile("docs/sample.xlsx"), "Hello from XLSX");
        assertContains(service.readDocumentFile("docs/sample.ipynb"), "Hello from IPYNB");
    }

    private void assertContains(DocumentReadResult result, String expected) {
        assertEquals("TEXT_EXTRACTED", result.status());
        assertTrue(result.success());
        assertTrue(result.extractedText().contains(expected));
    }

    private DocumentReadService service() {
        WorkspaceService workspaceService = new WorkspaceService(tempDir.resolve("workspace").toString());
        return new DocumentReadService(workspaceService, new GaiaAttachmentExtractionService(), audioTranscriptionService());
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
            var sheet = workbook.createSheet("Sheet1");
            Row row = sheet.createRow(0);
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
}
