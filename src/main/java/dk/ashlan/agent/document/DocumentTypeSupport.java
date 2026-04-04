package dk.ashlan.agent.document;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public final class DocumentTypeSupport {
    private static final Set<String> TEXT_LIKE_EXTENSIONS = Set.of(
            "txt",
            "md",
            "csv",
            "tsv",
            "json",
            "jsonl",
            "ndjson",
            "yaml",
            "yml",
            "html",
            "htm",
            "xml",
            "properties",
            "log",
            "ini",
            "rst",
            "toml",
            "java",
            "py",
            "js",
            "ts",
            "sql"
    );

    private static final Set<String> AUDIO_LIKE_EXTENSIONS = Set.of(
            "mp3",
            "wav",
            "m4a",
            "mp4",
            "mpeg",
            "mpga",
            "ogg",
            "webm",
            "flac"
    );

    private DocumentTypeSupport() {
    }

    public static String extension(Path path) {
        if (path == null || path.getFileName() == null) {
            return "";
        }
        return extension(path.getFileName().toString());
    }

    public static String extension(String value) {
        if (value == null) {
            return "";
        }
        int index = value.lastIndexOf('.');
        if (index < 0 || index == value.length() - 1) {
            return "";
        }
        return value.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean isTextLike(String extension) {
        return TEXT_LIKE_EXTENSIONS.contains(normalize(extension));
    }

    public static boolean isAudioLike(String extension) {
        return AUDIO_LIKE_EXTENSIONS.contains(normalize(extension));
    }

    public static boolean isSupportedTextDocument(String extension) {
        return isTextLike(extension) || "pdf".equals(normalize(extension));
    }

    private static String normalize(String extension) {
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }
}
