package dk.ashlan.agent.eval.gaia;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static dk.ashlan.agent.document.DocumentTypeSupport.extension;

@ApplicationScoped
public class GaiaAttachmentExtractionService {
    private static final int MAX_EXTRACTED_CHARS = 6000;
    private final ObjectMapper objectMapper;

    public GaiaAttachmentExtractionService() {
        this(new ObjectMapper());
    }

    @Inject
    public GaiaAttachmentExtractionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
                case "txt", "md", "csv", "tsv", "json", "jsonl", "ndjson", "yaml", "yml", "properties", "log", "ini", "rst", "toml", "java", "py", "js", "ts", "sql" ->
                        extractPlainText(path, fileName, fileType, "text/plain");
                case "html", "htm" -> extractMarkupText(path, fileName, fileType, "text/html");
                case "xml" -> extractMarkupText(path, fileName, fileType, "application/xml");
                case "pdf" -> extractPdf(path, fileName, fileType);
                case "docx" -> extractDocx(path, fileName, fileType);
                case "pptx" -> extractPptx(path, fileName, fileType);
                case "xlsx" -> extractXlsx(path, fileName, fileType);
                case "ipynb" -> extractNotebook(path, fileName, fileType);
                default -> new GaiaExtractedAttachment(
                        GaiaAttachmentStatus.UNSUPPORTED_TYPE,
                        "application/octet-stream",
                        fileType,
                        "",
                        "attachment type not supported for text extraction: " + fileType,
                        List.of("attachment:unsupported-type"),
                        false,
                        0,
                        0
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
                .replaceAll("(?is)<script[^>]*>.*?</script>", "\n")
                .replaceAll("(?is)<style[^>]*>.*?</style>", "\n")
                .replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)</(p|div|li|tr|h[1-6]|section|article|header|footer)>", "\n")
                .replaceAll("(?s)<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&");
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

    private GaiaExtractedAttachment extractDocx(Path path, String fileName, String fileType) throws IOException {
        try (InputStream input = Files.newInputStream(path); XWPFDocument document = new XWPFDocument(input)) {
            StringBuilder builder = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    builder.append(text.strip()).append("\n\n");
                }
            }
            return buildTextResult(fileName, fileType, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", builder.toString(), false);
        }
    }

    private GaiaExtractedAttachment extractPptx(Path path, String fileName, String fileType) throws IOException {
        try (InputStream input = Files.newInputStream(path); XMLSlideShow presentation = new XMLSlideShow(input)) {
            StringBuilder builder = new StringBuilder();
            int slideNumber = 1;
            for (XSLFSlide slide : presentation.getSlides()) {
                builder.append("Slide ").append(slideNumber++).append("\n");
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (text != null && !text.isBlank()) {
                            builder.append(text.strip()).append("\n");
                        }
                    }
                }
                builder.append("\n");
            }
            return buildTextResult(fileName, fileType, "application/vnd.openxmlformats-officedocument.presentationml.presentation", builder.toString(), false);
        }
    }

    private GaiaExtractedAttachment extractXlsx(Path path, String fileName, String fileType) throws IOException {
        try (InputStream input = Files.newInputStream(path); XSSFWorkbook workbook = new XSSFWorkbook(input)) {
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            StringBuilder builder = new StringBuilder();
            workbook.forEach(sheet -> {
                builder.append("Sheet ").append(sheet.getSheetName()).append("\n");
                for (Row row : sheet) {
                    StringBuilder rowBuilder = new StringBuilder();
                    for (Cell cell : row) {
                        String value = formatter.formatCellValue(cell);
                        if (value != null && !value.isBlank()) {
                            if (rowBuilder.length() > 0) {
                                rowBuilder.append(" | ");
                            }
                            rowBuilder.append(value.strip());
                        }
                    }
                    if (!rowBuilder.toString().isBlank()) {
                        builder.append(row.getRowNum() + 1).append(": ").append(rowBuilder).append("\n");
                    }
                }
                builder.append("\n");
            });
            return buildTextResult(fileName, fileType, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", builder.toString(), false);
        }
    }

    private GaiaExtractedAttachment extractNotebook(Path path, String fileName, String fileType) throws IOException {
        JsonNode root = objectMapper.readTree(Files.readString(path, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        JsonNode cells = root.path("cells");
        if (cells.isArray()) {
            int index = 1;
            for (JsonNode cell : cells) {
                String cellType = cell.path("cell_type").asText("");
                builder.append("Cell ").append(index++).append(" [").append(cellType).append("]\n");
                appendNotebookText(builder, cell.path("source"));
                JsonNode outputs = cell.path("outputs");
                if (outputs.isArray()) {
                    for (JsonNode output : outputs) {
                        appendNotebookOutput(builder, output);
                    }
                }
                builder.append("\n");
            }
        }
        return buildTextResult(fileName, fileType, "application/x-ipynb+json", builder.toString(), false);
    }

    private void appendNotebookText(StringBuilder builder, JsonNode source) {
        if (source == null) {
            return;
        }
        if (source.isArray()) {
            for (JsonNode line : source) {
                String value = line.asText("");
                if (!value.isBlank()) {
                    builder.append(value.stripTrailing());
                }
            }
            builder.append("\n");
            return;
        }
        String value = source.asText("");
        if (!value.isBlank()) {
            builder.append(value.strip()).append("\n");
        }
    }

    private void appendNotebookOutput(StringBuilder builder, JsonNode output) {
        if (output == null || output.isMissingNode()) {
            return;
        }
        JsonNode text = output.get("text");
        if (text != null) {
            builder.append("Output:\n");
            appendNotebookText(builder, text);
            return;
        }
        JsonNode data = output.get("data");
        if (data != null) {
            JsonNode plain = data.get("text/plain");
            if (plain != null) {
                builder.append("Output:\n");
                appendNotebookText(builder, plain);
            }
        }
    }

    private GaiaExtractedAttachment buildTextResult(String fileName, String fileType, String contentType, String text, boolean strippedMarkup) {
        String normalized = normalizeText(text);
        if (normalized.isBlank()) {
            return failure(fileName, fileType, "attachment text extraction produced no text", "attachment:text-extraction-failed");
        }
        List<String> events = new ArrayList<>();
        events.add("attachment:text-extracted");
        if (strippedMarkup) {
            events.add("attachment:text-normalized");
        }
        int originalLength = text == null ? 0 : text.length();
        int extractedLength = normalized.length();
        boolean wasTruncated = false;
        String note = "GAIA attachment text extracted from " + defaultText(fileName, fileType) + ".";
        if (extractedLength > MAX_EXTRACTED_CHARS) {
            normalized = normalized.substring(0, MAX_EXTRACTED_CHARS);
            extractedLength = normalized.length();
            wasTruncated = true;
            note = note + " Extracted text was truncated to " + MAX_EXTRACTED_CHARS + " characters.";
            events.add("attachment:text-truncated");
        }
        return new GaiaExtractedAttachment(
                GaiaAttachmentStatus.TEXT_EXTRACTED,
                contentType,
                fileType,
                normalized,
                note,
                List.copyOf(events),
                wasTruncated,
                originalLength,
                extractedLength
        );
    }

    private GaiaExtractedAttachment failure(String fileName, String fileType, String note, String event) {
        return new GaiaExtractedAttachment(
                GaiaAttachmentStatus.TEXT_EXTRACTION_FAILED,
                "application/octet-stream",
                fileType,
                "",
                note,
                List.of(event),
                false,
                0,
                0
        );
    }

    private String normalizeText(String input) {
        if (input == null) {
            return "";
        }
        String canonical = input.replace("\r\n", "\n").replace('\r', '\n');
        List<String> lines = canonical.lines()
                .map(line -> line.replaceAll("\\s+", " ").trim())
                .filter(line -> !line.isBlank())
                .toList();
        if (lines.isEmpty()) {
            return "";
        }
        return String.join("\n", lines);
    }

    private String extension(Path path) {
        return dk.ashlan.agent.document.DocumentTypeSupport.extension(path == null ? null : path.getFileName().toString());
    }

    private String defaultText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }
}
