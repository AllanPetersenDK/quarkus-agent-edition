package dk.ashlan.agent.eval.gaia;

import org.apache.avro.generic.GenericRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.parquet.avro.AvroParquetReader;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class HuggingFaceGaiaDatasetLoader implements GaiaDatasetLoader {
    private final ObjectMapper objectMapper;
    private final String datasetUrl;
    private final String token;

    @Inject
    public HuggingFaceGaiaDatasetLoader(
            ObjectMapper objectMapper,
            @ConfigProperty(name = "gaia.validation.dataset-url") String datasetUrl,
            @ConfigProperty(name = "gaia.validation.hf-token", defaultValue = "") String token
    ) {
        this.objectMapper = objectMapper;
        this.datasetUrl = datasetUrl;
        this.token = token;
    }

    @Override
    public List<GaiaValidationCase> load() {
        String effectiveUrl = datasetUrl == null ? "" : datasetUrl.trim();
        if (effectiveUrl.isBlank()) {
            throw new IllegalStateException("GAIA dataset URL is required. Set gaia.validation.dataset-url or GAIA_DATASET_URL.");
        }
        try {
            String normalizedUrl = normalizeHuggingFaceUrl(effectiveUrl);
            if (looksLikeParquet(normalizedUrl)) {
                return parseParquet(readBytes(normalizedUrl));
            }
            return parseText(readText(normalizedUrl));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load GAIA validation dataset from " + effectiveUrl, exception);
        }
    }

    @Override
    public String datasetUrl() {
        return datasetUrl;
    }

    private String normalizeHuggingFaceUrl(String url) {
        if (url.contains("huggingface.co/datasets/") && url.contains("/tree/")) {
            return url.replace("/tree/", "/resolve/");
        }
        return url;
    }

    private boolean looksLikeParquet(String url) {
        return url.toLowerCase().contains(".parquet");
    }

    private String readText(String url) throws IOException {
        if (url.startsWith("file:")) {
            return Files.readString(Path.of(URI.create(url)));
        }
        if (!url.contains("://")) {
            return Files.readString(Path.of(url));
        }
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .header("Accept", "application/json, application/x-ndjson, text/plain;q=0.9, */*;q=0.8");
            if (token != null && !token.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + token.trim());
            }
            HttpResponse<String> response = HttpClient.newHttpClient().send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                String message = "GAIA dataset request failed with HTTP " + response.statusCode() + " from " + url;
                if (response.statusCode() == 401 || response.statusCode() == 403) {
                    message += ". Set GAIA_HF_TOKEN/HF_TOKEN/HUGGINGFACE_TOKEN for protected Hugging Face datasets.";
                }
                throw new IOException(message);
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("GAIA dataset request interrupted", exception);
        }
    }

    private byte[] readBytes(String url) throws IOException {
        if (url.startsWith("file:")) {
            return Files.readAllBytes(Path.of(URI.create(url)));
        }
        if (!url.contains("://")) {
            return Files.readAllBytes(Path.of(url));
        }
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(url)).GET();
            if (token != null && !token.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + token.trim());
            }
            HttpResponse<byte[]> response = HttpClient.newHttpClient().send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                String message = "GAIA dataset request failed with HTTP " + response.statusCode() + " from " + url;
                if (response.statusCode() == 401 || response.statusCode() == 403) {
                    message += ". Set GAIA_HF_TOKEN/HF_TOKEN/HUGGINGFACE_TOKEN for protected Hugging Face datasets.";
                }
                throw new IOException(message);
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("GAIA dataset request interrupted", exception);
        }
    }

    private List<GaiaValidationCase> parseText(String body) throws IOException {
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.isBlank()) {
            return List.of();
        }
        if (trimmed.startsWith("[")) {
            JsonNode array = objectMapper.readTree(trimmed);
            List<GaiaValidationCase> cases = new ArrayList<>();
            for (JsonNode node : array) {
                cases.add(map(node));
            }
            return cases;
        }
        List<GaiaValidationCase> cases = new ArrayList<>();
        for (String line : trimmed.split("\\R")) {
            String cleaned = line.trim();
            if (!cleaned.isBlank()) {
                cases.add(map(objectMapper.readTree(cleaned)));
            }
        }
        return cases;
    }

    private List<GaiaValidationCase> parseParquet(byte[] bytes) throws IOException {
        java.nio.file.Path tempFile = Files.createTempFile("gaia-validation", ".parquet");
        try {
            Files.write(tempFile, bytes);
            try (var reader = AvroParquetReader.<GenericRecord>builder(new org.apache.hadoop.fs.Path(tempFile.toUri())).build()) {
                List<GaiaValidationCase> cases = new ArrayList<>();
                GenericRecord record;
                while ((record = reader.read()) != null) {
                    cases.add(map(record));
                }
                return cases;
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private GaiaValidationCase map(JsonNode node) {
        return new GaiaValidationCase(
                text(node, "task_id", "taskId", "id"),
                text(node, "Question", "question"),
                text(node, "Final answer", "final_answer", "finalAnswer", "answer"),
                text(node, "Level", "level"),
                text(node, "file_path", "filePath")
        );
    }

    private GaiaValidationCase map(GenericRecord record) {
        return new GaiaValidationCase(
                text(record, "task_id", "taskId", "id"),
                text(record, "Question", "question"),
                text(record, "Final answer", "final_answer", "finalAnswer", "answer"),
                text(record, "Level", "level"),
                text(record, "file_path", "filePath")
        );
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                String text = value.isArray() ? value.toString() : value.asText();
                if (text != null) {
                    return text;
                }
            }
        }
        return "";
    }

    private String text(GenericRecord record, String... names) {
        for (String name : names) {
            if (record.getSchema().getField(name) == null) {
                continue;
            }
            Object value = record.get(name);
            if (value != null) {
                return value.toString();
            }
        }
        return "";
    }
}
