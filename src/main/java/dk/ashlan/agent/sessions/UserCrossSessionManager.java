package dk.ashlan.agent.sessions;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class UserCrossSessionManager implements CrossSessionManager {
    private final TaskCrossSessionManager delegate = new TaskCrossSessionManager();

    @Override
    public void remember(String key, String value) {
        delegate.remember(key, value);
    }

    @Override
    public List<String> search(String query, int limit) {
        return delegate.search(query, limit);
    }
}
