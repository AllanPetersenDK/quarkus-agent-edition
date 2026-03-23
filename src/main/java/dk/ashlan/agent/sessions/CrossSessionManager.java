package dk.ashlan.agent.sessions;

import java.util.List;

public interface CrossSessionManager {
    void remember(String key, String value);

    List<String> search(String query, int limit);
}
