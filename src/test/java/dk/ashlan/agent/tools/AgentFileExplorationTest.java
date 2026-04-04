package dk.ashlan.agent.tools;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.eval.gaia.GaiaAttachmentExtractionService;
import dk.ashlan.agent.eval.gaia.GaiaAudioTranscriptionService;
import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.llm.LlmToolCall;
import dk.ashlan.agent.tools.filesystem.FilesystemToolService;
import dk.ashlan.agent.tools.filesystem.ListFilesTool;
import dk.ashlan.agent.tools.filesystem.ReadFileTool;
import dk.ashlan.agent.tools.filesystem.ReadMediaFileTool;
import dk.ashlan.agent.tools.filesystem.UnzipFileTool;
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

class AgentFileExplorationTest {
    @TempDir
    Path tempDir;

    @Test
    void agentCanExploreZipFilesSequentiallyUsingFilesystemTools() throws Exception {
        Path zip = tempDir.resolve("archive.zip");
        writeZip(zip, "docs/secret.txt", "The secret keyword is quarkus.");

        FilesystemToolService filesystemToolService = new FilesystemToolService(tempDir, new GaiaAttachmentExtractionService(), audioTranscriptionService());
        ToolRegistry toolRegistry = new ToolRegistry(List.of(
                new UnzipFileTool(filesystemToolService),
                new ListFilesTool(filesystemToolService),
                new ReadFileTool(filesystemToolService),
                new ReadMediaFileTool(filesystemToolService)
        ));
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        AtomicInteger calls = new AtomicInteger();
        LlmClient llmClient = (messages, registry, context) -> {
            int call = calls.getAndIncrement();
            if (call == 0) {
                return LlmCompletion.toolCalls(List.of(new LlmToolCall("unzip_file", Map.of("zipPath", "archive.zip", "extractTo", "unzipped/archive"), "call-1")));
            }
            if (call == 1) {
                return LlmCompletion.toolCalls(List.of(new LlmToolCall("list_files", Map.of("path", "unzipped/archive", "recursive", true, "maxEntries", 10), "call-2")));
            }
            if (call == 2) {
                return LlmCompletion.toolCalls(List.of(new LlmToolCall("read_file", Map.of("path", "unzipped/archive/docs/secret.txt"), "call-3")));
            }
            String extracted = messages.stream()
                    .filter(message -> "tool".equals(message.role()) && "read_file".equals(message.name()))
                    .reduce((first, second) -> second)
                    .map(LlmMessage::content)
                    .orElse("");
            return LlmCompletion.answer("The archive says: " + extracted);
        };

        AgentOrchestrator orchestrator = new AgentOrchestrator(llmClient, toolRegistry, toolExecutor, null, 4, "Use the file tools step by step.");

        AgentRunResult result = orchestrator.run("Find the secret keyword in the archive.", "file-session");

        assertTrue(result.finalAnswer().contains("quarkus"));
        assertTrue(result.trace().stream().anyMatch(entry -> entry.startsWith("tool:unzip_file:")));
        assertTrue(result.trace().stream().anyMatch(entry -> entry.startsWith("tool:list_files:")));
        assertTrue(result.trace().stream().anyMatch(entry -> entry.startsWith("tool:read_file:")));
        assertEquals(4, result.iterations());
    }

    private GaiaAudioTranscriptionService audioTranscriptionService() {
        return path -> "unused audio transcript";
    }

    private void writeZip(Path zipPath, String entryName, String content) throws Exception {
        Files.createDirectories(zipPath.getParent());
        try (java.util.zip.ZipOutputStream zipOutputStream = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipPath))) {
            java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(entryName);
            zipOutputStream.putNextEntry(entry);
            zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
    }
}
