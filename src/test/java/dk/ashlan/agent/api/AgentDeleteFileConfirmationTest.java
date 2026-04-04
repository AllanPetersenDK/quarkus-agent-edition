package dk.ashlan.agent.api;

import dk.ashlan.agent.api.dto.AgentRunRequest;
import dk.ashlan.agent.api.dto.AgentRunResponse;
import dk.ashlan.agent.code.WorkspaceService;
import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.core.ToolConfirmation;
import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;
import dk.ashlan.agent.tools.filesystem.DeleteFileTool;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentDeleteFileConfirmationTest {
    @TempDir
    Path tempDir;

    @Test
    void deleteRequestTriggersPendingConfirmationAndResumeDeletesFile() throws Exception {
        WorkspaceService workspaceService = new WorkspaceService(tempDir.resolve("workspace").toString());
        Path file = workspaceService.resolve("temp.txt");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "hello");

        ToolRegistry toolRegistry = new ToolRegistry(List.of(new DeleteFileTool(workspaceService)));
        AtomicInteger llmCalls = new AtomicInteger();
        LlmClient llmClient = (messages, registry, context) -> {
            llmCalls.incrementAndGet();
            return LlmCompletion.answer("The file was deleted.");
        };
        AgentOrchestrator orchestrator = new AgentOrchestrator(
                llmClient,
                toolRegistry,
                new ToolExecutor(toolRegistry),
                new MemoryService(new InMemoryTaskMemoryStore(), new MemoryExtractionService()),
                new SessionManager(),
                List.of(),
                3,
                ""
        );
        AgentResource resource = new AgentResource(orchestrator);

        AgentRunResponse pending = resource.runAgent(new AgentRunRequest("Slet filen temp.txt", "c6-hitl-deterministic"));

        assertEquals(StopReason.PENDING_CONFIRMATION, pending.stopReason());
        assertEquals(1, pending.pendingToolCalls().size());
        assertEquals("delete-file", pending.pendingToolCalls().get(0).toolName());
        assertEquals("temp.txt", pending.pendingToolCalls().get(0).arguments().get("path"));
        assertTrue(pending.pendingToolCalls().get(0).confirmationMessage().contains("temp.txt"));
        assertTrue(Files.exists(file));
        assertEquals(0, llmCalls.get());

        AgentRunResponse resumed = resource.runAgent(new AgentRunRequest(
                null,
                "c6-hitl-deterministic",
                List.of(ToolConfirmation.approved(
                        pending.pendingToolCalls().get(0).toolCallId(),
                        Map.of()
                ))
        ));

        assertEquals(StopReason.FINAL_ANSWER, resumed.stopReason());
        assertNotNull(resumed.answer());
        assertFalse(resumed.answer().isBlank());
        assertFalse(Files.exists(file));
        assertEquals(1, llmCalls.get());
    }
}
