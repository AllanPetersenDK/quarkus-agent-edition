package dk.ashlan.agent.eval.gaia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.example.GroupReadSupport;
import org.eclipse.microprofile.config.Config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class GaiaParquetLoader {
    private final ObjectMapper objectMapper;
    private final Config config;

    @Inject
    public GaiaParquetLoader(ObjectMapper objectMapper, Config config) {
        this.objectMapper = objectMapper;
        this.config = config;
    }

    public List<GaiaExample> load(String source) {
        String effectiveSource = source == null ? "" : source.trim();
        if (effectiveSource.isBlank()) {
            throw new IllegalStateException("GAIA dataset source is required.");
        }
        try {
            String normalizedSource = normalizeHuggingFaceUrl(effectiveSource);
            if (looksLikeParquet(normalizedSource)) {
                return parseParquet(readBytes(normalizedSource));
            }
            if (looksLikeJson(normalizedSource)) {
                return parseText(readText(normalizedSource));
            }
            if (normalizedSource.startsWith("file:")) {
                Path path = Path.of(URI.create(normalizedSource));
                if (Files.isDirectory(path)) {
                    throw new IllegalStateException("GAIA source points to a directory. The dataset service should resolve parquet files before loading.");
                }
                return loadPath(path);
            }
            Path path = Path.of(normalizedSource);
            if (Files.isDirectory(path)) {
                throw new IllegalStateException("GAIA source points to a directory. The dataset service should resolve parquet files before loading.");
            }
            if (Files.exists(path)) {
                return loadPath(path);
            }
            return parseText(readText(normalizedSource));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load GAIA dataset source " + effectiveSource, exception);
        }
    }

    private List<GaiaExample> loadPath(Path path) throws IOException {
        if (looksLikeParquet(path.toString())) {
            return parseParquet(Files.readAllBytes(path));
        }
        return parseText(Files.readString(path));
    }

    private String normalizeHuggingFaceUrl(String url) {
        if (url.contains("huggingface.co/datasets/") && url.contains("/tree/")) {
            return url.replace("/tree/", "/resolve/");
        }
        return url;
    }

    private boolean looksLikeParquet(String value) {
        return value.toLowerCase().contains(".parquet");
    }

    private boolean looksLikeJson(String value) {
        String lower = value.toLowerCase();
        return lower.endsWith(".json") || lower.endsWith(".jsonl") || lower.endsWith(".ndjson");
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
            String token = config.getOptionalValue("gaia.validation.hf-token", String.class).orElse("");
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
            String token = config.getOptionalValue("gaia.validation.hf-token", String.class).orElse("");
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

    private List<GaiaExample> parseText(String body) throws IOException {
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.isBlank()) {
            return List.of();
        }
        if (trimmed.startsWith("[")) {
            JsonNode array = objectMapper.readTree(trimmed);
            List<GaiaExample> cases = new ArrayList<>();
            for (JsonNode node : array) {
                cases.add(map(node));
            }
            return cases;
        }
        List<GaiaExample> cases = new ArrayList<>();
        for (String line : trimmed.split("\\R")) {
            String cleaned = line.trim();
            if (!cleaned.isBlank()) {
                cases.add(map(objectMapper.readTree(cleaned)));
            }
        }
        return cases;
    }

    private List<GaiaExample> parseParquet(byte[] bytes) throws IOException {
        java.nio.file.Path tempFile = Files.createTempFile("gaia-validation", ".parquet");
        try {
            Files.write(tempFile, bytes);
            try (ParquetReader<Group> reader = ParquetReader.builder(new GroupReadSupport(), new org.apache.hadoop.fs.Path(tempFile.toUri()))
                    .withConf(new Configuration(false))
                    .build()) {
                List<GaiaExample> cases = new ArrayList<>();
                Group record;
                while ((record = reader.read()) != null) {
                    cases.add(map(record));
                }
                return cases;
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private GaiaExample map(JsonNode node) {
        Map<String, Object> raw = objectMapper.convertValue(node, new com.fasterxml.jackson.core.type.TypeReference<>() {
        });
        return new GaiaExample(
                text(node, "task_id", "taskId", "id"),
                text(node, "Question", "question"),
                text(node, "Level", "level"),
                text(node, "file_name", "fileName"),
                text(node, "file_path", "filePath"),
                firstAnswer(node),
                listAnswers(node),
                raw,
                null
        );
    }

    private GaiaExample map(GenericRecord record) {
        Map<String, Object> raw = new LinkedHashMap<>();
        record.getSchema().getFields().forEach(field -> raw.put(field.name(), convertValue(record.get(field.name()))));
        return new GaiaExample(
                text(record, "task_id", "taskId", "id"),
                text(record, "Question", "question"),
                text(record, "Level", "level"),
                text(record, "file_name", "fileName"),
                text(record, "file_path", "filePath"),
                firstAnswer(record),
                listAnswers(record),
                raw,
                null
        );
    }

    private GaiaExample map(Group group) {
        Map<String, Object> raw = convertGroup(group);
        return new GaiaExample(
                text(group, "task_id", "taskId", "id"),
                text(group, "Question", "question"),
                text(group, "Level", "level"),
                text(group, "file_name", "fileName"),
                text(group, "file_path", "filePath"),
                firstAnswer(group),
                listAnswers(group),
                raw,
                null
        );
    }

    private Object convertValue(Object value) {
        if (value instanceof GenericRecord genericRecord) {
            Map<String, Object> map = new LinkedHashMap<>();
            genericRecord.getSchema().getFields().forEach(field -> map.put(field.name(), convertValue(genericRecord.get(field.name()))));
            return map;
        }
        if (value instanceof List<?> list) {
            List<Object> converted = new ArrayList<>();
            for (Object item : list) {
                converted.add(convertValue(item));
            }
            return converted;
        }
        return value;
    }

    private List<String> listAnswers(JsonNode node) {
        JsonNode value = firstNode(node, "golden_answers", "goldenAnswers", "Final answer", "final_answer", "finalAnswer", "answer");
        return listAnswersFromNode(value);
    }

    private List<String> listAnswers(GenericRecord record) {
        Object value = firstValue(record, "golden_answers", "goldenAnswers", "Final answer", "final_answer", "finalAnswer", "answer");
        return listAnswers(value);
    }

    private List<String> listAnswers(Group group) {
        Object value = firstValue(group, "golden_answers", "goldenAnswers", "Final answer", "final_answer", "finalAnswer", "answer");
        return listAnswers(value);
    }

    private List<String> listAnswersFromNode(JsonNode value) {
        if (value == null || value.isNull()) {
            return List.of();
        }
        if (value.isArray()) {
            List<String> answers = new ArrayList<>();
            for (JsonNode item : value) {
                String text = item.asText(null);
                if (text != null && !text.isBlank()) {
                    answers.add(text.trim());
                }
            }
            return List.copyOf(answers);
        }
        String text = value.asText(null);
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return List.of(text.trim());
    }

    private List<String> listAnswers(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<String> answers = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    String text = item.toString().trim();
                    if (!text.isBlank()) {
                        answers.add(text);
                    }
                }
            }
            return List.copyOf(answers);
        }
        String text = value.toString().trim();
        if (text.isBlank()) {
            return List.of();
        }
        return List.of(text);
    }

    private String firstAnswer(JsonNode node) {
        List<String> answers = listAnswers(node);
        return answers.isEmpty() ? "" : answers.getFirst();
    }

    private String firstAnswer(GenericRecord record) {
        List<String> answers = listAnswers(record);
        return answers.isEmpty() ? "" : answers.getFirst();
    }

    private String firstAnswer(Group group) {
        List<String> answers = listAnswers(group);
        return answers.isEmpty() ? "" : answers.getFirst();
    }

    private JsonNode firstNode(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    private Object firstValue(GenericRecord record, String... names) {
        for (String name : names) {
            if (record.getSchema().getField(name) == null) {
                continue;
            }
            Object value = record.get(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Object firstValue(Group group, String... names) {
        for (String name : names) {
            if (!group.getType().containsField(name) || group.getFieldRepetitionCount(name) == 0) {
                continue;
            }
            int fieldIndex = group.getType().getFieldIndex(name);
            if (group.getType().getType(fieldIndex).isPrimitive()) {
                return group.getValueToString(fieldIndex, 0);
            }
            return convertGroup((Group) group.getGroup(name, 0));
        }
        return null;
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                String text = value.asText();
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

    private String text(Group group, String... names) {
        for (String name : names) {
            if (!group.getType().containsField(name) || group.getFieldRepetitionCount(name) == 0) {
                continue;
            }
            int fieldIndex = group.getType().getFieldIndex(name);
            String text = group.getValueToString(fieldIndex, 0);
            if (text != null) {
                return text;
            }
        }
        return "";
    }

    private Map<String, Object> convertGroup(Group group) {
        Map<String, Object> raw = new LinkedHashMap<>();
        group.getType().getFields().forEach(field -> raw.put(field.getName(), convertField(group, field.getName())));
        return raw;
    }

    private Object convertField(Group group, String name) {
        if (!group.getType().containsField(name) || group.getFieldRepetitionCount(name) == 0) {
            return null;
        }
        int count = group.getFieldRepetitionCount(name);
        int fieldIndex = group.getType().getFieldIndex(name);
        if (group.getType().getType(fieldIndex).isPrimitive()) {
            if (count == 1) {
                return group.getValueToString(fieldIndex, 0);
            }
            List<Object> values = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                values.add(group.getValueToString(fieldIndex, i));
            }
            return values;
        }
        if (count == 1) {
            return convertGroup((Group) group.getGroup(name, 0));
        }
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            values.add(convertGroup((Group) group.getGroup(name, i)));
        }
        return values;
    }
}
