package dk.ashlan.agent.api;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.Path;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionChatCompletionsResourceTest {
    @Test
    void resourceIsTaggedAsAChapter02CompanionDebugSeam() throws Exception {
        Tag tag = CompanionChatCompletionsResource.class.getAnnotation(Tag.class);
        Path classPath = CompanionChatCompletionsResource.class.getAnnotation(Path.class);
        Method complete = CompanionChatCompletionsResource.class.getMethod(
                "complete",
                CompanionChatCompletionsResource.CompanionChatCompletionRequest.class
        );
        Operation operation = complete.getAnnotation(Operation.class);
        Path methodPath = complete.getAnnotation(Path.class);

        assertNotNull(tag);
        assertNotNull(classPath);
        assertNotNull(methodPath);
        assertEquals("Chapter 02 Companion Debug", tag.name());
        assertTrue(tag.description().contains("companion/debug seam"));
        assertEquals("/api/companion/llm", classPath.value());
        assertEquals("/completions", methodPath.value());
        assertNotNull(operation);
        assertTrue(operation.summary().contains("unified chat completion"));
        assertTrue(operation.description().contains("Book chapter: 2"));
        assertTrue(operation.description().contains("direct LLM API call via Swagger UI"));
        assertTrue(operation.description().contains("companion seam, not the main manual agent runtime API"));
    }
}
