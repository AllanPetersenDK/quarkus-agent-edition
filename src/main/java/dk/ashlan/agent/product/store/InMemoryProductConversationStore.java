package dk.ashlan.agent.product.store;

import dk.ashlan.agent.product.model.ProductConversationState;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@DefaultBean
@ApplicationScoped
public class InMemoryProductConversationStore implements ProductConversationStore {
    private final Map<String, ProductConversationState> conversations = new ConcurrentHashMap<>();

    @Override
    public Optional<ProductConversationState> load(String conversationId) {
        return Optional.ofNullable(conversations.get(conversationId));
    }

    @Override
    public ProductConversationState save(ProductConversationState state) {
        conversations.put(state.conversationId(), state);
        return state;
    }

    @Override
    public List<ProductConversationState> list(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<ProductConversationState> states = new ArrayList<>(conversations.values());
        states.sort(Comparator.comparing(ProductConversationState::updatedAt).reversed());
        return states.stream().limit(limit).toList();
    }

    @Override
    public long count() {
        return conversations.size();
    }
}
