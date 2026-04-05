package dk.ashlan.agent.api;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.Path;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter05SwaggerTest {
    @Test
    void ragResourceExposesChapterFiveSwaggerSurface() throws Exception {
        Tag tag = RagResource.class.getAnnotation(Tag.class);
        Path classPath = RagResource.class.getAnnotation(Path.class);
        Method ingest = RagResource.class.getMethod("ingest", RagResource.RagIngestRequest.class);
        Method ingestPath = RagResource.class.getMethod("ingestPath", RagResource.RagIngestPathRequest.class);
        Method ingestDirectory = RagResource.class.getMethod("ingestDirectory", RagResource.RagDirectoryIngestRequest.class);
        Method query = RagResource.class.getMethod("query", String.class, int.class);

        assertNotNull(tag);
        assertNotNull(classPath);
        assertEquals("/api/rag", classPath.value());
        assertTrue(tag.description().contains("RAG"));

        assertOperationContains(ingest, "Book chapter: 5");
        assertOperationContains(ingestPath, "Book chapter: 5");
        assertOperationContains(ingestDirectory, "Book chapter: 5");
        assertOperationContains(query, "Book chapter: 5");
    }

    private static void assertOperationContains(Method method, String expected) {
        Operation operation = method.getAnnotation(Operation.class);
        assertNotNull(operation, method.getName() + " is missing @Operation");
        assertTrue(operation.description().contains(expected), method.getName() + " should contain '" + expected + "'");
    }
}
