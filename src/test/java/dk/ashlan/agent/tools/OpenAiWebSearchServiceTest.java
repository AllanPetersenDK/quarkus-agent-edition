package dk.ashlan.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiWebSearchServiceTest {
    @Test
    void searchBuildsAResponsesWebSearchRequestAndParsesCompactSources() {
        AtomicReference<String> requestBody = new AtomicReference<>();
        OpenAiWebSearchService service = new OpenAiWebSearchService(
                "test-key",
                "gpt-4.1-mini",
                "http://example.com/v1",
                10,
                (uri, apiKey, payload, timeout) -> {
                    requestBody.set(payload);
                    assertEquals(Duration.ofSeconds(10), timeout);
                    assertEquals("test-key", apiKey);
                    assertTrue(uri.toString().endsWith("/responses"));
                    return new OpenAiWebSearchService.OpenAiResponse(200, """
                            {
                              "output": [
                                {
                                  "type": "web_search_call",
                                  "id": "ws_1",
                                  "status": "completed",
                                  "action": {
                                    "sources": [
                                      {
                                        "title": "Example source",
                                        "url": "https://example.com/source"
                                      }
                                    ]
                                  }
                                },
                                {
                                  "type": "message",
                                  "role": "assistant",
                                  "status": "completed",
                                  "content": [
                                    {
                                      "type": "output_text",
                                      "text": "Paris is the capital of France.",
                                      "annotations": [
                                        {
                                          "type": "url_citation",
                                          "url": "https://example.com/paris",
                                          "title": "Example Paris"
                                        }
                                      ]
                                    }
                                  ]
                                }
                              ]
                            }
                            """);
                },
                new ObjectMapper()
        );

        OpenAiWebSearchService.WebSearchResult result = service.search("What is the capital of France?");

        assertEquals("Paris is the capital of France.", result.summary());
        assertEquals(1, result.sources().size());
        assertEquals("Example Paris", result.sources().get(0).title());
        assertEquals("https://example.com/paris", result.sources().get(0).url());
        assertTrue(requestBody.get().contains("\"tools\":[{\"type\":\"web_search\"}]"));
        assertTrue(requestBody.get().contains("\"tool_choice\":\"auto\""));
        assertTrue(requestBody.get().contains("\"include\":[\"web_search_call.action.sources\"]"));
        assertTrue(requestBody.get().contains("\"input\":\"What is the capital of France?\""));
    }
}
