package dk.ashlan.agent.api;

import dk.ashlan.agent.api.dto.MemoryLookupRequest;
import dk.ashlan.agent.api.dto.MemoryLookupResponse;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.tools.ConversationSearchTool;
import dk.ashlan.agent.tools.RecallMemoryTool;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.Path;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeMemoryResourceTest {
    @Test
    void resourceExposesChapterSixMemoryRetrievalSeams() throws Exception {
        Tag tag = RuntimeMemoryResource.class.getAnnotation(Tag.class);
        Path classPath = RuntimeMemoryResource.class.getAnnotation(Path.class);
        Method recall = RuntimeMemoryResource.class.getMethod("recall", MemoryLookupRequest.class);
        Method conversationSearch = RuntimeMemoryResource.class.getMethod("conversationSearch", MemoryLookupRequest.class);
        Operation recallOperation = recall.getAnnotation(Operation.class);
        Operation searchOperation = conversationSearch.getAnnotation(Operation.class);

        assertNotNull(tag);
        assertNotNull(classPath);
        assertNotNull(recallOperation);
        assertNotNull(searchOperation);
        assertEquals("/api/runtime/memory", classPath.value());
        assertTrue(tag.description().contains("chapter-6"));
        assertTrue(recallOperation.description().contains("cross-session memory recall"));
        assertTrue(searchOperation.description().contains("session-oriented conversation lookup"));
    }

    @Test
    void recallAndConversationSearchReturnCompactOutputs() {
        SessionManager sessionManager = new SessionManager();
        sessionManager.session("session-a").addUserMessage("I prefer concise answers and PostgreSQL.");
        sessionManager.session("session-a").addAssistantMessage("Noted.");

        MemoryService memoryService = new MemoryService(sessionManager, new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        memoryService.remember("session-a", "goal", "Remember that my favorite database is PostgreSQL.");
        RuntimeMemoryResource resource = new RuntimeMemoryResource(
                new RecallMemoryTool(memoryService),
                new ConversationSearchTool(memoryService, sessionManager)
        );

        MemoryLookupResponse recall = resource.recall(new MemoryLookupRequest("session-a", "PostgreSQL"));
        MemoryLookupResponse conversation = resource.conversationSearch(new MemoryLookupRequest("session-a", "PostgreSQL"));

        assertEquals("recall-memory", recall.toolName());
        assertEquals("conversation-search", conversation.toolName());
        assertEquals("session-a", recall.sessionId());
        assertEquals("PostgreSQL", recall.query());
        assertTrue(recall.output().contains("PostgreSQL"));
        assertTrue(conversation.output().contains("user: I prefer concise answers and PostgreSQL."));
    }
}
