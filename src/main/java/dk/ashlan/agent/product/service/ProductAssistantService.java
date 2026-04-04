package dk.ashlan.agent.product.service;

import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.MemoryWriteDecision;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.memory.SessionState;
import dk.ashlan.agent.planning.ExecutionPlan;
import dk.ashlan.agent.planning.PlannerService;
import dk.ashlan.agent.planning.ReflectionResult;
import dk.ashlan.agent.planning.ReflectionService;
import dk.ashlan.agent.product.model.ProductAssistantQueryRequest;
import dk.ashlan.agent.product.model.ProductAssistantQueryResponse;
import dk.ashlan.agent.product.model.ProductPlanResponse;
import dk.ashlan.agent.product.model.ProductReflectionResponse;
import dk.ashlan.agent.product.model.ProductSourceResponse;
import dk.ashlan.agent.rag.RagService;
import dk.ashlan.agent.rag.RetrievalResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProductAssistantService {
    private static final int DEFAULT_TOP_K = 3;
    private final RagService ragService;
    private final MemoryService memoryService;
    private final SessionManager sessionManager;
    private final PlannerService plannerService;
    private final ReflectionService reflectionService;

    @Inject
    public ProductAssistantService(
            RagService ragService,
            MemoryService memoryService,
            SessionManager sessionManager,
            PlannerService plannerService,
            ReflectionService reflectionService
    ) {
        this.ragService = ragService;
        this.memoryService = memoryService;
        this.sessionManager = sessionManager;
        this.plannerService = plannerService;
        this.reflectionService = reflectionService;
    }

    public ProductAssistantQueryResponse query(ProductAssistantQueryRequest request) {
        String query = request.query() == null ? "" : request.query().trim();
        if (query.isBlank()) {
            throw new IllegalArgumentException("query is required");
        }
        String conversationId = normalizeConversationId(request.conversationId());
        int topK = request.topK() == null || request.topK() <= 0 ? DEFAULT_TOP_K : request.topK();

        SessionState session = sessionManager.session(conversationId);
        session.addUserMessage(query);

        List<RetrievalResult> retrieval = ragService.retrieve(query, topK);
        String answer = ragService.answer(query, retrieval);
        List<ProductSourceResponse> sources = retrieval.stream()
                .map(ProductSourceResponse::from)
                .toList();

        List<String> memoryHints = memoryService.relevantMemories(conversationId, query);
        ExecutionPlan plan = plannerService.plan(query);
        ReflectionResult reflection = reflectionService.reflect(answer);
        String finalAnswer = reflection.accepted() ? answer : reflection.revisedOutput();

        boolean canRemember = reflection.accepted() && !"No relevant knowledge found.".equals(finalAnswer);
        MemoryWriteDecision memoryWriteDecision = canRemember
                ? memoryService.remember(conversationId, "product assistant answer", "The answer is " + finalAnswer)
                : MemoryWriteDecision.SKIP;
        session.addAssistantMessage(finalAnswer);

        List<String> signals = new ArrayList<>();
        signals.add("product-query-start");
        signals.add("conversation:" + conversationId);
        signals.add("rag:retrieved:" + retrieval.size());
        signals.add("memory:hints:" + memoryHints.size());
        signals.add("plan:steps:" + plan.steps().size());
        signals.add("reflection:" + (reflection.accepted() ? "accepted" : "needs-work"));
        signals.add("memory-write:" + memoryWriteDecision.name().toLowerCase());
        signals.add("conversation:stored");

        return new ProductAssistantQueryResponse(
                conversationId,
                session.size(),
                query,
                finalAnswer,
                sources,
                memoryHints,
                new ProductPlanResponse(plan.formattedPlan(), plan.nextActiveStep() == null ? "" : plan.nextActiveStep().formattedLine(), plan.steps() == null ? 0 : plan.steps().size()),
                new ProductReflectionResponse(reflection.accepted(), reflection.feedback()),
                List.copyOf(signals)
        );
    }

    private String normalizeConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "product-" + UUID.randomUUID();
        }
        return conversationId.trim();
    }
}
