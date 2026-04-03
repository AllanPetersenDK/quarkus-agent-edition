package dk.ashlan.agent.llm;

import dk.ashlan.agent.core.ExecutionContext;
import dk.ashlan.agent.tools.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.ashlan.agent.rag.KnowledgeBaseTool;
import dk.ashlan.agent.rag.RagService;
import dk.ashlan.agent.tools.WebSearchTool;
import dk.ashlan.agent.tools.WikipediaTool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiLlmClientTest {
    @Test
    void completeReturnsAssistantAnswerFromOpenAiResponse() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<Duration> requestTimeout = new AtomicReference<>();
        OpenAiLlmClient.OpenAiTransport transport = (uri, apiKey, payload, timeout) -> {
            requestBody.set(payload);
            requestTimeout.set(timeout);
            return new OpenAiLlmClient.OpenAiResponse(200, """
                    {
                      "choices": [
                        {
                          "message": {
                            "role": "assistant",
                            "content": "Hello from OpenAI"
                          }
                        }
                      ]
                    }
                    """);
        };

        OpenAiLlmClient client = new OpenAiLlmClient("test-key", "gpt-4.1-mini", "http://example.com/v1", 10, transport, new ObjectMapper());
        LlmCompletion completion = client.complete(
                List.of(LlmMessage.system("You are helpful"), LlmMessage.user("Hello")),
                new ToolRegistry(List.of()),
                new ExecutionContext("Hello")
        );

        assertEquals("Hello from OpenAI", completion.content());
        assertTrue(completion.toolCalls().isEmpty());
        assertEquals(Duration.ofSeconds(10), requestTimeout.get());
        assertTrue(requestBody.get().contains("\"model\":\"gpt-4.1-mini\""));
        assertTrue(requestBody.get().contains("\"role\":\"system\""));
        assertTrue(requestBody.get().contains("\"content\":\"Hello\""));
        assertFalse(requestBody.get().contains("\"tool_choice\""));
    }

    @Test
    void completeParsesToolCallsFromOpenAiResponse() throws Exception {
        OpenAiLlmClient.OpenAiTransport transport = (uri, apiKey, payload, timeout) -> new OpenAiLlmClient.OpenAiResponse(200, """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "id": "assistant-call-1",
                        "tool_calls": [
                          {
                            "id": "call-123",
                            "type": "function",
                            "function": {
                              "name": "calculator",
                              "arguments": "{\\"expression\\":\\"25 * 4\\"}"
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
                """);

        OpenAiLlmClient client = new OpenAiLlmClient("test-key", "gpt-4.1-mini", "http://example.com/v1", 10, transport, new ObjectMapper());
        LlmCompletion completion = client.complete(
                List.of(LlmMessage.user("What is 25 * 4?")),
                new ToolRegistry(List.of()),
                new ExecutionContext("What is 25 * 4?")
        );

        assertTrue(completion.content() == null || completion.content().isBlank());
        assertFalse(completion.toolCalls().isEmpty());
        assertEquals("calculator", completion.toolCalls().get(0).toolName());
        assertEquals("25 * 4", completion.toolCalls().get(0).arguments().get("expression"));
        assertEquals("call-123", completion.toolCalls().get(0).callId());
    }

    @Test
    void completeSerializesToolMessagesWithToolCallId() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        OpenAiLlmClient.OpenAiTransport transport = (uri, apiKey, payload, timeout) -> {
            requestBody.set(payload);
            return new OpenAiLlmClient.OpenAiResponse(200, """
                    {
                      "choices": [
                        {
                          "message": {
                            "role": "assistant",
                            "content": "Done"
                          }
                        }
                      ]
                    }
                    """);
        };

        OpenAiLlmClient client = new OpenAiLlmClient("test-key", "gpt-4.1-mini", "http://example.com/v1", 10, transport, new ObjectMapper());
        client.complete(
                List.of(
                        LlmMessage.user("What is 25 * 4?"),
                        LlmMessage.tool("calculator", "call-123", "100")
                ),
                new ToolRegistry(List.of()),
                new ExecutionContext("What is 25 * 4?")
        );

        assertTrue(requestBody.get().contains("\"role\":\"tool\""));
        assertTrue(requestBody.get().contains("\"tool_call_id\":\"call-123\""));
        assertTrue(requestBody.get().contains("\"content\":\"100\""));
    }

    @Test
    void completeSerializesAssistantToolCallsBeforeToolResponses() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        OpenAiLlmClient.OpenAiTransport transport = (uri, apiKey, payload, timeout) -> {
            requestBody.set(payload);
            return new OpenAiLlmClient.OpenAiResponse(200, """
                    {
                      "choices": [
                        {
                          "message": {
                            "role": "assistant",
                            "content": "Done"
                          }
                        }
                      ]
                    }
                    """);
        };

        OpenAiLlmClient client = new OpenAiLlmClient("test-key", "gpt-4.1-mini", "http://example.com/v1", 10, transport, new ObjectMapper());
        client.complete(
                List.of(
                        LlmMessage.user("What is 25 * 4?"),
                        LlmMessage.assistant(List.of(new LlmToolCall("calculator", java.util.Map.of("expression", "25 * 4"), "call-123"))),
                        LlmMessage.tool("calculator", "call-123", "100")
                ),
                new ToolRegistry(List.of()),
                new ExecutionContext("What is 25 * 4?")
        );

        assertTrue(requestBody.get().contains("\"role\":\"assistant\""));
        assertTrue(requestBody.get().contains("\"tool_calls\""));
        assertTrue(requestBody.get().contains("\"tool_call_id\":\"call-123\""));
    }

    @Test
    void completeSerializesLookupToolDescriptionsAsLookupOnlyGuidance() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        OpenAiLlmClient.OpenAiTransport transport = (uri, apiKey, payload, timeout) -> {
            requestBody.set(payload);
            return new OpenAiLlmClient.OpenAiResponse(200, """
                    {
                      "choices": [
                        {
                          "message": {
                            "role": "assistant",
                            "content": "Direct answer"
                          }
                        }
                      ]
                    }
                    """);
        };

        RagService ragService = new RagService(null, null) {
            @Override
            public String answer(String query, int topK) {
                return "knowledge";
            }
        };
        OpenAiLlmClient client = new OpenAiLlmClient("test-key", "gpt-4.1-mini", "http://example.com/v1", 10, transport, new ObjectMapper());
        client.complete(
                List.of(LlmMessage.user("What is the capital of France?")),
                new ToolRegistry(List.of(
                        new KnowledgeBaseTool(ragService),
                        new WebSearchTool(),
                        new WikipediaTool()
                )),
                new ExecutionContext("What is the capital of France?")
        );

        assertTrue(requestBody.get().contains("Search the in-memory knowledge base for repo or RAG questions. Not for simple stable facts or basic general knowledge."));
        assertTrue(requestBody.get().contains("Search the web for current, external, or explicitly requested lookup tasks. Not for simple stable facts."));
        assertTrue(requestBody.get().contains("Search Wikipedia when the user explicitly asks for Wikipedia or needs a sourced lookup. Not for simple stable facts."));
    }
}
