package dk.ashlan.agent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

class LlmClientSelectorTest {
    @Test
    void autoPrefersOpenAiWhenApiKeyIsConfigured() {
        OpenAiLlmClient openAi = new OpenAiLlmClient(
                "key",
                "gpt-4.1-mini",
                "http://example.com/v1",
                10,
                (uri, apiKey, payload, timeout) -> new OpenAiLlmClient.OpenAiResponse(200, "{}"),
                new ObjectMapper()
        );
        LangChain4jLlmClient langChain4j = new LangChain4jLlmClient(request -> "framework");
        DemoToolCallingLlmClient demo = new DemoToolCallingLlmClient();

        LlmClient selected = LlmClientSelector.select(List.of(demo, openAi, langChain4j), "auto", "key");

        assertSame(openAi, selected);
    }

    @Test
    void autoFallsBackToDemoWhenNoApiKeyIsConfigured() {
        OpenAiLlmClient openAi = new OpenAiLlmClient(
                "",
                "gpt-4.1-mini",
                "http://example.com/v1",
                10,
                (uri, apiKey, payload, timeout) -> new OpenAiLlmClient.OpenAiResponse(200, "{}"),
                new ObjectMapper()
        );
        LangChain4jLlmClient langChain4j = new LangChain4jLlmClient(request -> "framework");
        DemoToolCallingLlmClient demo = new DemoToolCallingLlmClient();

        LlmClient selected = LlmClientSelector.select(List.of(openAi, langChain4j, demo), "auto", "");

        assertSame(demo, selected);
    }

    @Test
    void explicitLangChain4jSelectsTheFrameworkSeam() {
        OpenAiLlmClient openAi = new OpenAiLlmClient(
                "key",
                "gpt-4.1-mini",
                "http://example.com/v1",
                10,
                (uri, apiKey, payload, timeout) -> new OpenAiLlmClient.OpenAiResponse(200, "{}"),
                new ObjectMapper()
        );
        LangChain4jLlmClient langChain4j = new LangChain4jLlmClient(request -> "framework");

        LlmClient selected = LlmClientSelector.select(List.of(openAi, langChain4j), "langchain4j", "key");

        assertSame(langChain4j, selected);
    }
}
