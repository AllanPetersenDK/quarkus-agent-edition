package dk.ashlan.agent.eval.gaia;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetWriter;

import java.nio.file.Path;
import java.util.List;

final class GaiaTestSupport {
    private GaiaTestSupport() {
    }

    static void writeParquet(Path file, List<GaiaRow> rows) throws Exception {
        Schema schema = new Schema.Parser().parse("""
                {
                  "type": "record",
                  "name": "GaiaCase",
                  "fields": [
                    {"name": "task_id", "type": "string"},
                    {"name": "question", "type": "string"},
                    {"name": "final_answer", "type": "string"},
                    {"name": "level", "type": "string"},
                    {"name": "file_name", "type": "string"},
                    {"name": "file_path", "type": "string"}
                  ]
                }
                """);
        try (var writer = AvroParquetWriter.<GenericRecord>builder(new org.apache.hadoop.fs.Path(file.toUri()))
                .withSchema(schema)
                .build()) {
            for (GaiaRow row : rows) {
                GenericRecord record = new GenericData.Record(schema);
                record.put("task_id", row.taskId());
                record.put("question", row.question());
                record.put("final_answer", row.finalAnswer());
                record.put("level", row.level());
                record.put("file_name", row.fileName());
                record.put("file_path", row.filePath());
                writer.write(record);
            }
        }
    }

    record GaiaRow(String taskId, String question, String finalAnswer, String level, String fileName, String filePath) {
    }
}
