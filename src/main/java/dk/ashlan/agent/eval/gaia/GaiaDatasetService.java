package dk.ashlan.agent.eval.gaia;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class GaiaDatasetService {
    private final GaiaParquetLoader parquetLoader;
    private final GaiaAttachmentResolver attachmentResolver;

    @Inject
    public GaiaDatasetService(GaiaParquetLoader parquetLoader, GaiaAttachmentResolver attachmentResolver) {
        this.parquetLoader = parquetLoader;
        this.attachmentResolver = attachmentResolver;
    }

    public List<GaiaExample> load(GaiaDatasetSelection selection) {
        String source = selection.resolvedSource();
        if (source.isBlank()) {
            throw new IllegalStateException("GAIA dataset source is required. Set gaia.validation.dataset-url or gaia.validation.local-path.");
        }
        List<String> resolvedSources = resolveSources(source, selection.config(), selection.split(), selection.level());
        List<GaiaExample> examples = new ArrayList<>();
        for (String resolvedSource : resolvedSources) {
            String attachmentBase = sourceBase(resolvedSource);
            examples.addAll(parquetLoader.load(resolvedSource).stream()
                    .map(example -> example.withAttachment(attachmentResolver.resolve(example, attachmentBase)))
                    .toList());
        }
        List<GaiaExample> resolved = examples.stream()
                .filter(example -> matchesLevel(example.level(), selection.level()))
                .sorted(Comparator.comparing(GaiaExample::taskId, Comparator.nullsLast(String::compareTo)))
                .toList();
        int limit = selection.limit() == null || selection.limit() < 1 ? resolved.size() : selection.limit();
        return resolved.stream().limit(limit).toList();
    }

    private List<String> resolveSources(String source, String config, String split, Integer level) {
        if (source.contains("://")) {
            return List.of(resolveRemoteSource(source, config, split, level));
        }
        Path path = source.startsWith("file:") ? Path.of(java.net.URI.create(source)) : Path.of(source);
        if (Files.isRegularFile(path)) {
            return List.of(path.toString());
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalStateException("GAIA dataset source does not exist: " + source);
        }
        Path candidateDir = path;
        if (config != null && !config.isBlank()) {
            Path configured = path.resolve(config.trim());
            if (Files.isDirectory(configured)) {
                candidateDir = configured;
            }
        }
        if (split != null && !split.isBlank()) {
            Path splitDir = candidateDir.resolve(split.trim());
            if (Files.isDirectory(splitDir)) {
                candidateDir = splitDir;
            }
        }
        if (level != null && level > 0) {
            Path levelFile = candidateDir.resolve("metadata.level" + level + ".parquet");
            if (Files.isRegularFile(levelFile)) {
                return List.of(levelFile.toString());
            }
        }
        try (var walk = Files.walk(candidateDir)) {
            return walk.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".parquet"))
                    .sorted()
                    .map(Path::toString)
                    .toList();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to enumerate GAIA parquet files under " + candidateDir, exception);
        }
    }

    private String resolveRemoteSource(String source, String config, String split, Integer level) {
        if (source.toLowerCase(Locale.ROOT).contains(".parquet")) {
            return normalize(source);
        }
        if (config == null || config.isBlank() || split == null || split.isBlank()) {
            return normalize(source);
        }
        String normalized = normalize(source);
        String fileName = level != null && level > 0 ? "metadata.level" + level + ".parquet" : "metadata.parquet";
        String base = normalized.endsWith("/") ? normalized : normalized + "/";
        if (normalized.contains("/resolve/")) {
            return base + config.trim() + "/" + split.trim() + "/" + fileName;
        }
        return base + config.trim() + "/" + split.trim() + "/" + fileName;
    }

    private String normalize(String source) {
        if (source.contains("huggingface.co/datasets/") && source.contains("/tree/")) {
            return source.replace("/tree/", "/resolve/");
        }
        return source;
    }

    private boolean matchesLevel(String levelValue, Integer selectedLevel) {
        if (selectedLevel == null || selectedLevel < 1) {
            return true;
        }
        if (levelValue == null || levelValue.isBlank()) {
            return selectedLevel == 1;
        }
        String digits = levelValue.replaceAll("[^0-9]", "");
        return digits.equals(String.valueOf(selectedLevel));
    }

    private String sourceBase(String source) {
        if (source.startsWith("file:")) {
            Path path = Path.of(java.net.URI.create(source));
            Path base = Files.isDirectory(path) ? path : path.getParent();
            return base == null ? path.toString() : base.toString();
        }
        if (source.contains("://")) {
            int index = source.lastIndexOf('/');
            return index > source.indexOf("://") + 2 ? source.substring(0, index) : source;
        }
        Path path = Path.of(source);
        Path base = Files.isDirectory(path) ? path : path.getParent();
        return base == null ? path.toString() : base.toString();
    }
}
