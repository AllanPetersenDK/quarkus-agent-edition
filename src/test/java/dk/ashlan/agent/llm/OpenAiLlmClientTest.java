package dk.ashlan.agent.llm;

import dk.ashlan.agent.core.ExecutionContext;
import dk.ashlan.agent.tools.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiLlmClientTest {
    @Test
    void completeReturnsAssistantAnswerFromOpenAiResponse() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        OpenAiLlmClient.OpenAiTransport transport = (uri, apiKey, payload, timeout) -> {
            requestBody.set(payload);
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
        assertTrue(requestBody.get().contains("\"model\":\"gpt-4.1-mini\""));
        assertTrue(requestBody.get().contains("\"role\":\"system\""));
        assertTrue(requestBody.get().contains("\"content\":\"Hello\""));
    }

    @Test
    void completeParsesToolCallsFromOpenAiResponse() throws Exception {
        OpenAiLlmClient.OpenAiTransport transport = (uri, apiKey, payload, timeout) -> new OpenAiLlmClient.OpenAiResponse(200, """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "tool_calls": [
                          {
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
    }
}
