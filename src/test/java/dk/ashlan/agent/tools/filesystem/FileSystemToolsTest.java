package dk.ashlan.agent.tools.filesystem;

import dk.ashlan.agent.eval.gaia.GaiaAttachmentExtractionService;
import dk.ashlan.agent.eval.gaia.GaiaAudioTranscriptionService;
import dk.ashlan.agent.tools.JsonToolResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemToolsTest {
    @TempDir
    Path tempDir;

    @Test
    void unzipFileExtractsZipAndListFilesShowsStructure() throws Exception {
        Path zip = tempDir.resolve("sample.zip");
        writeZip(zip,
                "docs/readme.txt", "Hello from readme",
                "nested/info.md", "# Nested info",
                "nested/extra.txt", "Extra file"
        );

        FilesystemToolService service = service(path -> "unused");
        JsonToolResult unzip = new UnzipFileTool(service).execute(Map.of("zipPath", "sample.zip", "extractTo", "unzipped/sample"));

        assertTrue(unzip.success());
        assertTrue(unzip.output().contains("extractedFiles=3"));
        assertTrue(Files.exists(tempDir.resolve("unzipped/sample/docs/readme.txt")));

        JsonToolResult list = new ListFilesTool(service).execute(Map.of("path", "unzipped/sample", "recursive", true, "maxEntries", 2));

        assertTrue(list.success());
        List<?> entries = (List<?>) list.data().get("entries");
        assertTrue(entries.size() == 2);
        assertTrue(entries.stream().anyMatch(entry -> entry.toString().contains("unzipped/sample/docs/readme.txt")));
        assertTrue(Boolean.TRUE.equals(list.data().get("truncated")));
    }

    @Test
    void listFilesCanReturnNonTruncatedCompactView() throws Exception {
        Files.writeString(tempDir.resolve("alpha.txt"), "A");
        Files.writeString(tempDir.resolve("beta.txt"), "B");

        FilesystemToolService service = service(path -> "unused");
        JsonToolResult list = new ListFilesTool(service).execute(Map.of("path", ".", "recursive", false, "maxEntries", 10));

        assertTrue(list.success());
        assertTrue(Boolean.FALSE.equals(list.data().get("truncated")));
    }

    @Test
    void inspectPathReportsBasicMetadata() throws Exception {
        Files.writeString(tempDir.resolve("notes.txt"), "Hello text");

        FilesystemToolService service = service(path -> "unused");

        JsonToolResult inspectFile = new InspectPathTool(service).execute(Map.of("path", "notes.txt"));
        JsonToolResult inspectMissing = new InspectPathTool(service).execute(Map.of("path", "missing.txt"));

        assertTrue(inspectFile.success());
        assertTrue(inspectFile.output().contains("kind=file"));
        assertTrue(inspectFile.output().contains("extension=txt"));
        assertTrue(new InspectPathTool(service).execute(Map.of("path", ".")).output().contains("kind=folder"));
        assertTrue(inspectMissing.success());
        assertTrue(inspectMissing.output().contains("kind=missing"));
    }

    @Test
    void readFileReadsSupportedTextFormats() throws Exception {
        Files.writeString(tempDir.resolve("notes.txt"), "Hello text");
        Files.writeString(tempDir.resolve("notes.md"), "# Hello markdown");
        Files.writeString(tempDir.resolve("data.csv"), "name,value\nalpha,1");
        Files.writeString(tempDir.resolve("data.json"), "{\"name\":\"alpha\"}");
        Files.writeString(tempDir.resolve("page.html"), "<html><body>Hello <strong>HTML</strong></body></html>");
        Files.writeString(tempDir.resolve("doc.xml"), "<root><title>Hello XML</title></root>");

        FilesystemToolService service = service(path -> "unused");

        assertTrue(new ReadFileTool(service).execute(Map.of("path", "notes.txt")).output().contains("Hello text"));
        assertTrue(new ReadFileTool(service).execute(Map.of("path", "notes.md")).output().contains("Hello markdown"));
        assertTrue(new ReadFileTool(service).execute(Map.of("path", "data.csv")).output().contains("alpha,1"));
        assertTrue(new ReadFileTool(service).execute(Map.of("path", "data.json")).output().contains("\"name\":\"alpha\""));
        assertTrue(new ReadFileTool(service).execute(Map.of("path", "page.html")).output().contains("Hello HTML"));
        assertTrue(new ReadFileTool(service).execute(Map.of("path", "doc.xml")).output().contains("Hello XML"));
    }

    @Test
    void pdfAndAudioPathsUseRelevantExtractionFlow() throws Exception {
        Path pdf = tempDir.resolve("document.pdf");
        writePdf(pdf, "PDF hello world");
        Files.writeString(tempDir.resolve("clip.mp3"), "not a real audio but enough for the path");

        AtomicBoolean audioCalled = new AtomicBoolean(false);
        FilesystemToolService service = service(path -> {
            audioCalled.set(true);
            return "transcribed audio";
        });

        JsonToolResult pdfResult = new ReadMediaFileTool(service).execute(Map.of("path", "document.pdf"));
        assertTrue(pdfResult.success());
        assertTrue(pdfResult.output().contains("PDF hello world"));
        assertFalse(audioCalled.get());

        JsonToolResult audioResult = new ReadMediaFileTool(service).execute(Map.of("path", "clip.mp3"));
        assertTrue(audioResult.success());
        assertTrue(audioResult.output().contains("transcribed audio"));
        assertTrue(audioCalled.get());

        JsonToolResult pdfAliasResult = new ReadDocumentFileTool(service).execute(Map.of("path", "document.pdf"));
        assertTrue(pdfAliasResult.success());
        assertTrue(pdfAliasResult.output().contains("PDF hello world"));
    }

    @Test
    void guardrailBlocksReadsOutsideAllowedRoot() throws Exception {
        FilesystemToolService service = service(path -> "unused");

        JsonToolResult result = new ReadFileTool(service).execute(Map.of("path", "../outside.txt"));

        assertFalse(result.success());
        assertTrue(result.output().contains("Path traversal is not allowed"));
    }

    private FilesystemToolService service(GaiaAudioTranscriptionService audioTranscriptionService) {
        return new FilesystemToolService(tempDir, new GaiaAttachmentExtractionService(), audioTranscriptionService);
    }

    private void writeZip(Path zipPath, String... entries) throws IOException {
        Files.createDirectories(zipPath.getParent());
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (int index = 0; index < entries.length; index += 2) {
                ZipEntry entry = new ZipEntry(entries[index]);
                zipOutputStream.putNextEntry(entry);
                zipOutputStream.write(entries[index + 1].getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }
        }
    }

    private void writePdf(Path pdfPath, String text) throws IOException {
        Files.createDirectories(pdfPath.getParent());
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
            document.save(pdfPath.toFile());
        }
    }
}
