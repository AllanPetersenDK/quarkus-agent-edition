package dk.ashlan.agent.api;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.Path;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter06SwaggerTest {
    @Test
    void runtimeContextResourceExposesChapterSixSwaggerSurface() throws Exception {
        Tag tag = RuntimeContextResource.class.getAnnotation(Tag.class);
        Path classPath = RuntimeContextResource.class.getAnnotation(Path.class);
        Method optimize = RuntimeContextResource.class.getMethod("optimize", dk.ashlan.agent.api.dto.ContextOptimizeRequest.class);
        Method slidingWindow = RuntimeContextResource.class.getMethod("slidingWindow", dk.ashlan.agent.api.dto.ContextOptimizeRequest.class);

        assertNotNull(tag);
        assertNotNull(classPath);
        assertEquals("/api/runtime/context", classPath.value());
        assertTrue(tag.description().contains("chapter-6"));

        assertOperationContains(optimize, "Book chapter mapping: chapter 6");
        assertOperationContains(slidingWindow, "Book chapter mapping: chapter 6");
    }

    @Test
    void runtimeMemoryResourceExposesChapterSixSwaggerSurface() throws Exception {
        Tag tag = RuntimeMemoryResource.class.getAnnotation(Tag.class);
        Path classPath = RuntimeMemoryResource.class.getAnnotation(Path.class);
        Method recall = RuntimeMemoryResource.class.getMethod("recall", dk.ashlan.agent.api.dto.MemoryLookupRequest.class);
        Method conversationSearch = RuntimeMemoryResource.class.getMethod("conversationSearch", dk.ashlan.agent.api.dto.MemoryLookupRequest.class);

        assertNotNull(tag);
        assertNotNull(classPath);
        assertEquals("/api/runtime/memory", classPath.value());
        assertTrue(tag.description().contains("chapter-6"));

        assertOperationContains(recall, "Book chapter mapping: chapter 6");
        assertOperationContains(conversationSearch, "Book chapter mapping: chapter 6");
    }

    private static void assertOperationContains(Method method, String expected) {
        Operation operation = method.getAnnotation(Operation.class);
        assertNotNull(operation, method.getName() + " is missing @Operation");
        assertTrue(operation.description().contains(expected), method.getName() + " should contain '" + expected + "'");
    }
}
