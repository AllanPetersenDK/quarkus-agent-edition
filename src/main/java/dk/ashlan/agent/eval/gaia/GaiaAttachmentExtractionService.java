package dk.ashlan.agent.eval.gaia;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class GaiaAttachmentExtractionService {
    private static final int MAX_EXTRACTED_CHARS = 6000;

    public GaiaExtractedAttachment extract(Path path) {
        if (path == null) {
            return failure("", "", "attachment path is null", "attachment:text-extraction-failed");
        }
        if (!Files.isRegularFile(path)) {
            return failure(path.getFileName().toString(), extension(path), "attachment file does not exist: " + path, "attachment:text-extraction-failed");
        }

        String fileName = path.getFileName().toString();
        String fileType = extension(path);
        try {
            return switch (fileType) {
                case "txt", "md", "csv", "json", "jsonl", "ndjson", "yaml", "yml" ->
                        extractPlainText(path, fileName, fileType, "text/plain");
                case "html", "htm" -> extractMarkupText(path, fileName, fileType, "text/html");
                case "xml" -> extractMarkupText(path, fileName, fileType, "application/xml");
                case "pdf" -> extractPdf(path, fileName, fileType);
                default -> new GaiaExtractedAttachment(
                        GaiaAttachmentStatus.UNSUPPORTED_TYPE,
                        "application/octet-stream",
                        fileType,
                        "",
                        "attachment type not supported for text extraction: " + fileType,
                        List.of("attachment:unsupported-type")
                );
            };
        } catch (Exception exception) {
            return failure(fileName, fileType, "attachment text extraction failed: " + exception.getMessage(), "attachment:text-extraction-failed");
        }
    }

    private GaiaExtractedAttachment extractPlainText(Path path, String fileName, String fileType, String contentType) throws IOException {
        String raw = Files.readString(path, StandardCharsets.UTF_8);
        return buildTextResult(fileName, fileType, contentType, raw, false);
    }

    private GaiaExtractedAttachment extractMarkupText(Path path, String fileName, String fileType, String contentType) throws IOException {
        String raw = Files.readString(path, StandardCharsets.UTF_8);
        String stripped = raw
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?s)<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return buildTextResult(fileName, fileType, contentType, stripped, true);
    }

    private GaiaExtractedAttachment extractPdf(Path path, String fileName, String fileType) throws IOException {
        try (PDDocument document = PDDocument.load(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String extracted = stripper.getText(document);
            return buildTextResult(fileName, fileType, "application/pdf", extracted, false);
        }
    }

    private GaiaExtractedAttachment buildTextResult(String fileName, String fileType, String contentType, String text, boolean strippedMarkup) {
        String normalized = normalizeWhitespace(text);
        if (normalized.isBlank()) {
            return failure(fileName, fileType, "attachment text extraction produced no text", "attachment:text-extraction-failed");
        }
        List<String> events = new java.util.ArrayList<>();
        events.add("attachment:text-extracted");
        if (strippedMarkup) {
            events.add("attachment:text-normalized");
        }
        String note = "GAIA attachment text extracted from " + defaultText(fileName, fileType) + ".";
        if (normalized.length() > MAX_EXTRACTED_CHARS) {
            normalized = normalized.substring(0, MAX_EXTRACTED_CHARS);
            note = note + " Extracted text was truncated to " + MAX_EXTRACTED_CHARS + " characters.";
            events.add("attachment:text-truncated");
        }
        return new GaiaExtractedAttachment(
                GaiaAttachmentStatus.TEXT_EXTRACTED,
                contentType,
                fileType,
                normalized,
                note,
                List.copyOf(events)
        );
    }

    private GaiaExtractedAttachment failure(String fileName, String fileType, String note, String event) {
        return new GaiaExtractedAttachment(
                GaiaAttachmentStatus.TEXT_EXTRACTION_FAILED,
                "application/octet-stream",
                fileType,
                "",
                note,
                List.of(event)
        );
    }

    private String normalizeWhitespace(String input) {
        return input == null ? "" : input.replaceAll("\\s+", " ").trim();
    }

    private String extension(Path path) {
        String name = path == null ? "" : path.getFileName().toString();
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) {
            return "";
        }
        return name.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String defaultText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }
}
