package dk.ashlan.agent.chapters.chapter05;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.eval.gaia.GaiaAttachmentExtractionService;
import dk.ashlan.agent.eval.gaia.GaiaAudioTranscriptionService;
import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.llm.LlmToolCall;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;
import dk.ashlan.agent.tools.filesystem.FilesystemToolService;
import dk.ashlan.agent.tools.filesystem.InspectPathTool;
import dk.ashlan.agent.tools.filesystem.ListFilesTool;
import dk.ashlan.agent.tools.filesystem.ReadDocumentFileTool;
import dk.ashlan.agent.tools.filesystem.UnzipFileTool;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter05FileExplorationDemoTest {
    @TempDir
    Path tempDir;

    @Test
    void demoReturnsSingleAnswerAfterSequentialFileExploration() throws Exception {
        Path zip = tempDir.resolve("archive.zip");
        Path pdf = tempDir.resolve("archive/docs/answer.pdf");
        writePdf(pdf, "The secret keyword is quarkus.");
        writeZip(zip, "docs/answer.pdf", pdf);

        FilesystemToolService filesystemToolService = new FilesystemToolService(tempDir, new GaiaAttachmentExtractionService(), audioTranscriptionService());
        ToolRegistry toolRegistry = new ToolRegistry(List.of(
                new InspectPathTool(filesystemToolService),
                new UnzipFileTool(filesystemToolService),
                new ListFilesTool(filesystemToolService),
                new ReadDocumentFileTool(filesystemToolService)
        ));
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        AtomicInteger calls = new AtomicInteger();
        LlmClient llmClient = (messages, registry, context) -> {
            int call = calls.getAndIncrement();
            if (call == 0) {
                return LlmCompletion.toolCalls(List.of(new LlmToolCall("inspect_path", Map.of("path", "archive.zip"), "call-1")));
            }
            if (call == 1) {
                return LlmCompletion.toolCalls(List.of(new LlmToolCall("unzip_file", Map.of("zipPath", "archive.zip", "extractTo", "unzipped/archive"), "call-2")));
            }
            if (call == 2) {
                return LlmCompletion.toolCalls(List.of(new LlmToolCall("list_files", Map.of("path", "unzipped/archive", "recursive", true, "maxEntries", 10), "call-3")));
            }
            if (call == 3) {
                return LlmCompletion.toolCalls(List.of(new LlmToolCall("read_document_file", Map.of("path", "unzipped/archive/docs/answer.pdf"), "call-4")));
            }
            String extracted = messages.stream()
                    .filter(message -> "tool".equals(message.role()) && "read_document_file".equals(message.name()))
                    .reduce((first, second) -> second)
                    .map(LlmMessage::content)
                    .orElse("");
            return LlmCompletion.answer("The archive says: " + extracted);
        };

        AgentOrchestrator orchestrator = new AgentOrchestrator(llmClient, toolRegistry, toolExecutor, null, 5, "Chapter 5 file exploration mode.");
        Chapter05FileExplorationDemo demo = new Chapter05FileExplorationDemo(orchestrator);

        Chapter05FileExplorationDemo.Chapter05FileExplorationResult result = demo.run("What is the secret keyword?", "archive.zip");

        assertEquals("What is the secret keyword?", result.question());
        assertEquals("archive.zip", result.path());
        assertTrue(result.answer().contains("quarkus"));
        assertTrue(result.trace().stream().anyMatch(entry -> entry.startsWith("tool:inspect_path:")));
        assertTrue(result.trace().stream().anyMatch(entry -> entry.startsWith("tool:read_document_file:")));
    }

    private GaiaAudioTranscriptionService audioTranscriptionService() {
        return path -> "unused audio transcript";
    }

    private void writeZip(Path zipPath, String entryName, Path sourceFile) throws Exception {
        Files.createDirectories(zipPath.getParent());
        try (java.util.zip.ZipOutputStream zipOutputStream = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipPath))) {
            java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(entryName);
            zipOutputStream.putNextEntry(entry);
            zipOutputStream.write(Files.readAllBytes(sourceFile));
            zipOutputStream.closeEntry();
        }
    }

    private void writePdf(Path pdfPath, String text) throws Exception {
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
