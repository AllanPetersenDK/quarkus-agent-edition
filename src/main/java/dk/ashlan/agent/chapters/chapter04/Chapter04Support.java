package dk.ashlan.agent.chapters.chapter04;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.CallbackAwareAgentOrchestrator;
import dk.ashlan.agent.core.StructuredOutputAgentOrchestrator;
import dk.ashlan.agent.llm.DemoToolCallingLlmClient;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ClockTool;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;

import java.util.List;

final class Chapter04Support {
    private Chapter04Support() {
    }

    static AgentOrchestrator orchestrator() {
        ToolRegistry toolRegistry = new ToolRegistry(List.of(new CalculatorTool(), new ClockTool()));
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        MemoryService memoryService = new MemoryService(
                new SessionManager(),
                new InMemoryTaskMemoryStore(),
                new MemoryExtractionService()
        );
        return new AgentOrchestrator(
                new DemoToolCallingLlmClient(),
                toolRegistry,
                toolExecutor,
                memoryService,
                4,
                "You are a chapter 04 demo agent."
        );
    }

    static CallbackAwareAgentOrchestrator callbackOrchestrator() {
        return new CallbackAwareAgentOrchestrator(orchestrator());
    }

    static StructuredOutputAgentOrchestrator structuredOutputOrchestrator() {
        return new StructuredOutputAgentOrchestrator();
    }
}
