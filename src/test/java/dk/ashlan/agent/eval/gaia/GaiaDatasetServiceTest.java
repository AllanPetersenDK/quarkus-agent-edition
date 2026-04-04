package dk.ashlan.agent.eval.gaia;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GaiaDatasetServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void filtersByLevelAndLimitAndResolvesAttachments() throws Exception {
        Path snapshotRoot = tempDir.resolve("snapshot");
        Path validationDir = snapshotRoot.resolve("2023/validation");
        Files.createDirectories(validationDir);
        Files.writeString(validationDir.resolve("attachment.txt"), "GAIA attachment content");
        GaiaTestSupport.writeParquet(validationDir.resolve("metadata.level1.parquet"), List.of(
                new GaiaTestSupport.GaiaRow("task-1", "What is PostgreSQL?", "PostgreSQL", "1", "attachment.txt", "2023/validation/attachment.txt"),
                new GaiaTestSupport.GaiaRow("task-2", "What is H2?", "H2", "2", "ignored.txt", "2023/validation/ignored.txt")
        ));

        GaiaDatasetService service = new GaiaDatasetService(
                new GaiaParquetLoader(new ObjectMapper(), new SmallRyeConfigBuilder().build()),
                new GaiaAttachmentResolver(path -> {
                    throw new AssertionError("audio transcription should not be used for text attachments");
                })
        );

        List<GaiaExample> examples = service.load(new GaiaDatasetSelection(snapshotRoot.toString(), "", "2023", "validation", 1, 1, false));

        assertEquals(1, examples.size());
        assertEquals("task-1", examples.getFirst().taskId());
        assertTrue(examples.getFirst().attachment().present());
        assertTrue(examples.getFirst().attachment().note().contains("GAIA attachment text preview"));
    }

    @Test
    void failsClearlyWhenSourceIsMissing() {
        GaiaDatasetService service = new GaiaDatasetService(
                new GaiaParquetLoader(new ObjectMapper(), new SmallRyeConfigBuilder().build()),
                new GaiaAttachmentResolver(path -> {
                    throw new AssertionError("audio transcription should not be used for missing-source validation");
                })
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                service.load(new GaiaDatasetSelection("", "", "2023", "validation", 1, 1, false))
        );

        assertTrue(exception.getMessage().contains("GAIA dataset source is required"));
    }
}
