package dk.ashlan.agent.eval.gaia;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GaiaAttachmentExtractionServiceTest {
    @TempDir
    Path tempDir;

    @ParameterizedTest
    @MethodSource("textFixtures")
    void extractsSupportedTextAttachments(String fileName, String content, String expectedSnippet) throws Exception {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, content, StandardCharsets.UTF_8);

        GaiaExtractedAttachment extracted = new GaiaAttachmentExtractionService().extract(file);

        assertEquals(GaiaAttachmentStatus.TEXT_EXTRACTED, extracted.status());
        assertTrue(extracted.extractedText().contains(expectedSnippet));
        assertTrue(extracted.traceEvents().contains("attachment:text-extracted"));
    }

    @Test
    void extractsPdfTextWithoutOcr() throws Exception {
        Path file = tempDir.resolve("sample.pdf");
        writePdf(file, "PDF attachment says hello world.");

        GaiaExtractedAttachment extracted = new GaiaAttachmentExtractionService().extract(file);

        assertEquals(GaiaAttachmentStatus.TEXT_EXTRACTED, extracted.status());
        assertTrue(extracted.extractedText().contains("PDF attachment says hello world."));
        assertTrue(extracted.traceEvents().contains("attachment:text-extracted"));
    }

    @Test
    void reportsExtractionFailureForBrokenPdf() throws Exception {
        Path file = tempDir.resolve("broken.pdf");
        Files.writeString(file, "not a real pdf", StandardCharsets.UTF_8);

        GaiaExtractedAttachment extracted = new GaiaAttachmentExtractionService().extract(file);

        assertEquals(GaiaAttachmentStatus.TEXT_EXTRACTION_FAILED, extracted.status());
        assertTrue(extracted.traceEvents().contains("attachment:text-extraction-failed"));
    }

    @Test
    void reportsUnsupportedTypeExplicitly() throws Exception {
        Path file = tempDir.resolve("sample.bin");
        Files.writeString(file, "binary-ish", StandardCharsets.UTF_8);

        GaiaExtractedAttachment extracted = new GaiaAttachmentExtractionService().extract(file);

        assertEquals(GaiaAttachmentStatus.UNSUPPORTED_TYPE, extracted.status());
        assertTrue(extracted.traceEvents().contains("attachment:unsupported-type"));
    }

    private static Stream<Arguments> textFixtures() {
        return Stream.of(
                Arguments.of("sample.txt", "Plain text attachment.", "Plain text attachment."),
                Arguments.of("sample.md", "# Heading\nMarkdown attachment.", "Markdown attachment."),
                Arguments.of("sample.csv", "name,value\nfoo,bar", "foo,bar"),
                Arguments.of("sample.json", "{\"hello\":\"world\"}", "\"hello\":\"world\""),
                Arguments.of("sample.html", "<html><body><h1>Title</h1><p>HTML attachment.</p></body></html>", "Title HTML attachment."),
                Arguments.of("sample.xml", "<root><item>XML attachment.</item></root>", "XML attachment.")
        );
    }

    private void writePdf(Path file, String text) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            document.save(file.toFile());
        }
    }
}
