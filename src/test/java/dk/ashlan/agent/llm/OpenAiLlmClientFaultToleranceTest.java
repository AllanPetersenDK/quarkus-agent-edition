package dk.ashlan.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiLlmClientFaultToleranceTest {
    private static final String SUCCESS_BODY = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "Recovered"
                  }
                }
              ]
            }
    """;

    private TestOpenAiApi api;
    private OpenAiLlmClient.DefaultOpenAiTransport transport;

    @BeforeEach
    void setUp() {
        api = new TestOpenAiApi();
        transport = new OpenAiLlmClient.DefaultOpenAiTransport(api);
    }

    @Test
    void retriesTransientProviderFailures() throws Exception {
        api.mode = Mode.RETRY_THEN_SUCCESS;

        OpenAiLlmClient.OpenAiResponse response = transport.post(
                URI.create("http://example.com/v1/chat/completions"),
                "test-key",
                "{}",
                Duration.ofSeconds(1)
        );

        assertTrue(response.body().contains("\"content\": \"Recovered\""));
        assertEquals(3, api.invocations.get());
    }

    @Test
    void timesOutSlowProviderCalls() {
        api.mode = Mode.SLOW;

        OpenAiLlmClient.OpenAiTransientException exception = assertThrows(OpenAiLlmClient.OpenAiTransientException.class, () -> transport.post(
                URI.create("http://example.com/v1/chat/completions"),
                "test-key",
                "{}",
                Duration.ofMillis(100)
        ));

        assertTrue(exception.getMessage().contains("timed out"));
    }

    @Test
    void doesNotRetryPermanentProviderFailures() {
        api.mode = Mode.PERMANENT_ERROR;

        assertThrows(OpenAiLlmClient.OpenAiPermanentException.class, () -> transport.post(
                URI.create("http://example.com/v1/chat/completions"),
                "test-key",
                "{}",
                Duration.ofSeconds(1)
        ));

        assertEquals(1, api.invocations.get());
    }

    @Test
    void preservesTheRequestedTimeoutBoundary() {
        api.mode = Mode.SLOW;

        long started = System.nanoTime();
        assertThrows(OpenAiLlmClient.OpenAiTransientException.class, () -> transport.post(
                URI.create("http://example.com/v1/chat/completions"),
                "test-key",
                "{}",
                Duration.ofMillis(100)
        ));
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertTrue(elapsed < 1000, "timeout should stop the call promptly");
    }

    enum Mode {
        SUCCESS,
        RETRY_THEN_SUCCESS,
        SLOW,
        PERMANENT_ERROR
    }

    static final class TestOpenAiApi implements OpenAiLlmClient.OpenAiApi {
        private final AtomicInteger invocations = new AtomicInteger();
        private volatile Mode mode = Mode.SUCCESS;

        @Override
        public Response chatCompletions(String authorization, JsonNode payload) {
            int call = invocations.incrementAndGet();
            return switch (mode) {
                case SUCCESS -> ok();
                case RETRY_THEN_SUCCESS -> call < 3 ? transientFailure() : ok();
                case SLOW -> slow();
                case PERMANENT_ERROR -> permanentFailure();
            };
        }

        private Response ok() {
            return Response.status(200).entity(SUCCESS_BODY).build();
        }

        private Response transientFailure() {
            return Response.status(503).entity("{\"error\":\"temporary upstream failure\"}").build();
        }

        private Response permanentFailure() {
            return Response.status(401).entity("{\"error\":\"invalid api key\"}").build();
        }

        private Response slow() {
            try {
                Thread.sleep(250);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return ok();
        }
    }
}
