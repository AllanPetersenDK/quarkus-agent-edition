package dk.ashlan.agent.eval.gaia;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GaiaParquetLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsCasesFromLocalParquetFixture() throws Exception {
        Path parquet = tempDir.resolve("metadata.level1.parquet");
        GaiaTestSupport.writeParquet(parquet, List.of(
                new GaiaTestSupport.GaiaRow("task-1", "What is GAIA?", "42", "1", "doc.txt", "doc.txt")
        ));

        GaiaParquetLoader loader = new GaiaParquetLoader(new ObjectMapper(), new SmallRyeConfigBuilder().build());

        List<GaiaExample> examples = loader.load(parquet.toString());

        assertEquals(1, examples.size());
        assertEquals("task-1", examples.getFirst().taskId());
        assertTrue(examples.getFirst().expectedAnswers().contains("42"));
    }
}
