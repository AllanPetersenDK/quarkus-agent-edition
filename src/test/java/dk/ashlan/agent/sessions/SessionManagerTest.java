package dk.ashlan.agent.sessions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SessionManagerTest {
    @Test
    void inMemorySessionManagerRetainsSessionState() {
        InMemorySessionManager manager = new InMemorySessionManager();
        Session session = manager.getOrCreate("session-1");
        session.addEvent("hello");

        assertEquals("hello", manager.getOrCreate("session-1").events().get(0));
        assertEquals("session-1", session.sessionId());
        assertEquals("active", session.state());
        assertNotNull(session.createdAt());
        assertNotNull(session.updatedAt());
    }
}
