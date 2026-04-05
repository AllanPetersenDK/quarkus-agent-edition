package dk.ashlan.agent.product.store;

import dk.ashlan.agent.product.model.ProductConversationState;

import java.util.List;
import java.util.Optional;

public interface ProductConversationStore {
    Optional<ProductConversationState> load(String conversationId);

    ProductConversationState save(ProductConversationState state);

    List<ProductConversationState> list(int limit);

    long count();
}
