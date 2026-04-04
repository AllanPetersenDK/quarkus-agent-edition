package dk.ashlan.agent.eval.gaia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class OpenAiGaiaAudioTranscriptionService implements GaiaAudioTranscriptionService {
    private static final String DEFAULT_MODEL = "gpt-4o-mini-transcribe";
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Duration requestTimeout;
    private final TranscriptionTransport transport;
    private final ObjectMapper objectMapper;

    @Inject
    public OpenAiGaiaAudioTranscriptionService(Config config) {
        this(
                config.getOptionalValue("openai.api-key", String.class).orElse(""),
                config.getOptionalValue("gaia.validation.audio-transcription-model", String.class).orElse(DEFAULT_MODEL),
                config.getOptionalValue("openai.base-url", String.class).orElse(DEFAULT_BASE_URL),
                config.getOptionalValue("openai.timeout-seconds", Integer.class).orElse(DEFAULT_TIMEOUT_SECONDS),
                new DefaultTranscriptionTransport(),
                new ObjectMapper()
        );
    }

    OpenAiGaiaAudioTranscriptionService(
            String apiKey,
            String model,
            String baseUrl,
            int timeoutSeconds,
            TranscriptionTransport transport,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.requestTimeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.transport = Objects.requireNonNull(transport, "transport");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public String transcribe(Path audioFile) {
        if (audioFile == null) {
            throw new IllegalArgumentException("Audio file must not be null.");
        }
        if (!Files.isRegularFile(audioFile)) {
            throw new IllegalArgumentException("Audio file does not exist: " + audioFile);
        }
        if (apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured for GAIA audio transcription.");
        }

        try {
            MultipartPayload payload = buildMultipartPayload(audioFile);
            OpenAiResponse response = transport.post(
                    URI.create(baseUrl + "/audio/transcriptions"),
                    apiKey,
                    payload.body(),
                    payload.contentType(),
                    requestTimeout
            );
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("OpenAI audio transcription failed with HTTP " + response.statusCode() + ": " + response.body());
            }
            return parseTranscript(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("OpenAI audio transcription response could not be prepared or parsed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI audio transcription request was interrupted", exception);
        }
    }

    private MultipartPayload buildMultipartPayload(Path audioFile) throws IOException {
        String boundary = "----gaia-transcription-" + UUID.randomUUID();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeTextPart(output, boundary, "model", model);
        byte[] bytes = Files.readAllBytes(audioFile);
        String fileName = audioFile.getFileName().toString();
        String contentType = Files.probeContentType(audioFile);
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }
        writeFilePart(output, boundary, "file", fileName, contentType, bytes);
        writeLine(output, "--" + boundary + "--");
        writeLine(output, "");
        return new MultipartPayload(output.toByteArray(), "multipart/form-data; boundary=" + boundary);
    }

    private void writeTextPart(ByteArrayOutputStream output, String boundary, String name, String value) throws IOException {
        writeLine(output, "--" + boundary);
        writeLine(output, "Content-Disposition: form-data; name=\"" + name + "\"");
        writeLine(output, "");
        writeLine(output, value);
    }

    private void writeFilePart(ByteArrayOutputStream output, String boundary, String name, String fileName, String contentType, byte[] bytes) throws IOException {
        writeLine(output, "--" + boundary);
        writeLine(output, "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"");
        writeLine(output, "Content-Type: " + contentType);
        writeLine(output, "");
        output.write(bytes);
        writeLine(output, "");
    }

    private void writeLine(ByteArrayOutputStream output, String line) throws IOException {
        output.write(line.getBytes(StandardCharsets.UTF_8));
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String parseTranscript(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        String text = root.path("text").asText("");
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("OpenAI audio transcription response did not include transcript text.");
        }
        return normalizeWhitespace(text);
    }

    private String normalizeWhitespace(String input) {
        return input == null ? "" : input.replaceAll("\\s+", " ").trim();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl.trim();
    }

    interface TranscriptionTransport {
        OpenAiResponse post(URI uri, String apiKey, byte[] payload, String contentType, Duration timeout) throws IOException, InterruptedException;
    }

    static final class DefaultTranscriptionTransport implements TranscriptionTransport {
        private final HttpClient httpClient = HttpClient.newBuilder().build();

        @Override
        public OpenAiResponse post(URI uri, String apiKey, byte[] payload, String contentType, Duration timeout) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new OpenAiResponse(response.statusCode(), response.body());
        }
    }

    record OpenAiResponse(int statusCode, String body) {
    }

    record MultipartPayload(byte[] body, String contentType) {
    }
}
