package dk.ashlan.agent.chapters.chapter02;

import dk.ashlan.agent.core.ExecutionContext;
import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.LlmRequestBuilder;
import dk.ashlan.agent.llm.DemoToolCallingLlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.llm.StructuredOutputParser;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ClockTool;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;

import java.util.List;

final class Chapter02Support {
    private Chapter02Support() {
    }

    static ConversationManagementDemo conversationDemo() {
        return new ConversationManagementDemo();
    }

    static ToolRegistry toolRegistry() {
        return new ToolRegistry(List.of(new CalculatorTool(), new ClockTool()));
    }

    static ToolExecutor toolExecutor() {
        return new ToolExecutor(toolRegistry());
    }

    static MemoryService memoryService() {
        return new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService());
    }

    static LlmRequestBuilder requestBuilder() {
        return new LlmRequestBuilder("You are a chapter 02 demo agent.", memoryService());
    }

    static List<LlmMessage> requestMessages(String input) {
        ExecutionContext context = new ExecutionContext(input, "chapter-02");
        return requestBuilder().build(context);
    }

    static DemoToolCallingLlmClient llmClient() {
        return new DemoToolCallingLlmClient();
    }

    static StructuredOutputParser parser() {
        return new StructuredOutputParser();
    }

    static LlmCompletion complete(String input) {
        ExecutionContext context = new ExecutionContext(input, "chapter-02");
        return llmClient().complete(requestMessages(input), toolRegistry(), context);
    }

    static AgentOrchestrator orchestrator() {
        return new AgentOrchestrator(
                llmClient(),
                toolRegistry(),
                toolExecutor(),
                memoryService(),
                4,
                "You are a chapter 02 demo agent."
        );
    }
}
