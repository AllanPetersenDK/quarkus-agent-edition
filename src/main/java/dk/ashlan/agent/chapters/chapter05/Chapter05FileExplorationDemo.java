package dk.ashlan.agent.chapters.chapter05;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.llm.LlmMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class Chapter05FileExplorationDemo {
    private final AgentOrchestrator orchestrator;

    @Inject
    public Chapter05FileExplorationDemo(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public Chapter05FileExplorationResult run(String question, String path) {
        String effectiveQuestion = question == null ? "" : question.trim();
        String effectivePath = path == null ? "" : path.trim();
        List<LlmMessage> supplementalMessages = List.of(LlmMessage.system("""
                Chapter 5 file exploration mode.
                Use inspect_path, list_files, unzip_file, read_file, and read_document_file step by step.
                Start by inspecting the provided path, explore the archive or folder incrementally, and answer with the most relevant file-backed evidence.
                Stay inside the workspace root and do not guess.
                Target path: %s
                """.formatted(effectivePath)));
        AgentRunResult result = orchestrator.run(effectiveQuestion, "chapter5-file-exploration-" + UUID.randomUUID(), supplementalMessages);
        return new Chapter05FileExplorationResult(effectiveQuestion, effectivePath, result.finalAnswer(), result.stopReason().name(), result.iterations(), result.trace());
    }

    public record Chapter05FileExplorationResult(
            String question,
            String path,
            String answer,
            String stopReason,
            int iterations,
            List<String> trace
    ) {
    }
}
