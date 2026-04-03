package dk.ashlan.agent.tools;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.llm.LlmToolCall;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentOrchestratorWebSearchTest {
    @Test
    void webDependentQuestionCanTriggerWebSearchThroughTheManualRuntimeLoop() {
        AtomicInteger calls = new AtomicInteger();
        OpenAiWebSearchService webSearchService = new OpenAiWebSearchService(
                "test-key",
                "gpt-4.1-mini",
                "http://example.com/v1",
                10,
                (uri, apiKey, payload, timeout) -> new OpenAiWebSearchService.OpenAiResponse(200, """
                        {
                          "output": [
                            {
                              "type": "message",
                              "role": "assistant",
                              "status": "completed",
                              "content": [
                                {
                                  "type": "output_text",
                                  "text": "Weather looks rainy today.",
                                  "annotations": [
                                    {
                                      "type": "url_citation",
                                      "url": "https://example.com/weather",
                                      "title": "Example Weather"
                                    }
                                  ]
                                }
                              ]
                            }
                          ]
                        }
                        """),
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
        WebSearchTool webSearchTool = new WebSearchTool(webSearchService);
        ToolRegistry toolRegistry = new ToolRegistry(List.of(webSearchTool));
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        MemoryService memoryService = new MemoryService(
                new SessionManager(),
                new InMemoryTaskMemoryStore(),
                new MemoryExtractionService()
        );

        LlmClient llmClient = (messages, registry, context) -> {
            if (calls.getAndIncrement() == 0) {
                return LlmCompletion.toolCalls(List.of(new LlmToolCall(
                        "web-search",
                        Map.of("query", "latest weather in Copenhagen"),
                        "call-1"
                )));
            }
            String webSearchOutput = messages.stream()
                    .filter(message -> "tool".equals(message.role()) && "web-search".equals(message.name()))
                    .reduce((first, second) -> second)
                    .map(LlmMessage::content)
                    .orElse("");
            return LlmCompletion.answer("The runtime loop used web-search: " + webSearchOutput);
        };

        AgentOrchestrator orchestrator = new AgentOrchestrator(llmClient, toolRegistry, toolExecutor, memoryService, 3, "system prompt");

        AgentRunResult result = orchestrator.run("What is the latest weather in Copenhagen?", "session-1");

        assertEquals("The runtime loop used web-search: Weather looks rainy today.\nSources: [1] Example Weather - https://example.com/weather", result.finalAnswer());
        assertTrue(result.trace().stream().anyMatch(entry -> entry.startsWith("tool:web-search:")));
    }
}
