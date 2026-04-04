package dk.ashlan.agent.eval.gaia;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HuggingFaceGaiaDatasetLoaderParquetTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsParquetFilesAndMapsGaiaFields() throws Exception {
        Path parquetFile = tempDir.resolve("metadata.level1.parquet");
        writeParquet(parquetFile);

        HuggingFaceGaiaDatasetLoader loader = new HuggingFaceGaiaDatasetLoader(
                new com.fasterxml.jackson.databind.ObjectMapper(),
                parquetFile.toString(),
                ""
        );

        List<GaiaValidationCase> cases = loader.load();

        assertEquals(1, cases.size());
        assertEquals("task-1", cases.getFirst().taskId());
        assertEquals("What is GAIA?", cases.getFirst().question());
        assertEquals("42", cases.getFirst().finalAnswer());
        assertEquals("1", cases.getFirst().level());
    }

    private void writeParquet(Path file) throws Exception {
        Schema schema = new Schema.Parser().parse("""
                {
                  "type": "record",
                  "name": "GaiaCase",
                  "fields": [
                    {"name": "task_id", "type": "string"},
                    {"name": "question", "type": "string"},
                    {"name": "final_answer", "type": "string"},
                    {"name": "level", "type": "string"},
                    {"name": "file_path", "type": "string"}
                  ]
                }
                """);
        GenericRecord record = new GenericData.Record(schema);
        record.put("task_id", "task-1");
        record.put("question", "What is GAIA?");
        record.put("final_answer", "42");
        record.put("level", "1");
        record.put("file_path", "");

        try (var writer = AvroParquetWriter.<GenericRecord>builder(new org.apache.hadoop.fs.Path(file.toUri()))
                .withSchema(schema)
                .build()) {
            writer.write(record);
        }
    }
}
