package dk.ashlan.agent.eval.gaia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiGaiaAudioTranscriptionServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void transcribeBuildsMultipartRequestAndParsesTranscript() throws Exception {
        Path audioFile = tempDir.resolve("sample.mp3");
        Files.writeString(audioFile, "fake audio bytes");
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();

        OpenAiGaiaAudioTranscriptionService service = new OpenAiGaiaAudioTranscriptionService(
                "test-key",
                "gpt-4o-mini-transcribe",
                "http://example.com/v1",
                15,
                (uri, apiKey, payload, requestContentType, timeout) -> {
                    assertEquals("test-key", apiKey);
                    assertEquals(Duration.ofSeconds(15), timeout);
                    assertEquals(URI.create("http://example.com/v1/audio/transcriptions"), uri);
                    requestBody.set(new String(payload, StandardCharsets.UTF_8));
                    contentType.set(requestContentType);
                    return new OpenAiGaiaAudioTranscriptionService.OpenAiResponse(200, """
                            {"text":"Hello from audio"}
                            """);
                },
                new ObjectMapper()
        );

        String transcript = service.transcribe(audioFile);

        assertEquals("Hello from audio", transcript);
        assertTrue(contentType.get().startsWith("multipart/form-data; boundary="));
        assertTrue(requestBody.get().contains("Content-Disposition: form-data; name=\"model\""));
        assertTrue(requestBody.get().contains("gpt-4o-mini-transcribe"));
        assertTrue(requestBody.get().contains("filename=\"sample.mp3\""));
        assertTrue(requestBody.get().contains("fake audio bytes"));
    }
}
