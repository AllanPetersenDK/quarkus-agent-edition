package dk.ashlan.agent.tools;

import java.util.Map;
import java.util.stream.Collectors;

public final class SchemaUtils {
    private SchemaUtils() {
    }

    public static String toSchemaJson(Map<String, ToolDefinition> definitions) {
        return definitions.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":\"" + entry.getValue().description() + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }
}
