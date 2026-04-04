package dk.ashlan.agent.product.service;

import dk.ashlan.agent.eval.RuntimeRunRecorder;
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
import dk.ashlan.agent.product.model.ProductConversationState;
import dk.ashlan.agent.product.model.ProductConversationTurn;
import dk.ashlan.agent.product.model.ProductPlanResponse;
import dk.ashlan.agent.product.model.ProductReflectionResponse;
import dk.ashlan.agent.product.model.ProductSourceResponse;
import dk.ashlan.agent.product.api.ProductApiException;
import dk.ashlan.agent.product.store.InMemoryProductConversationStore;
import dk.ashlan.agent.product.store.ProductConversationStore;
import dk.ashlan.agent.rag.RagService;
import dk.ashlan.agent.rag.RetrievalResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProductAssistantService {
    private static final int DEFAULT_TOP_K = 3;
    private static final int MAX_TOP_K = 10;
    private static final int MAX_QUERY_LENGTH = 4096;
    private static final int MAX_CONVERSATION_ID_LENGTH = 128;
    private static final String CONVERSATION_ID_PATTERN = "^[A-Za-z0-9._:-]+$";
    private final RagService ragService;
    private final MemoryService memoryService;
    private final SessionManager sessionManager;
    private final PlannerService plannerService;
    private final ReflectionService reflectionService;
    private final RuntimeRunRecorder runRecorder;
    private final ProductConversationStore conversationStore;

    @Inject
    public ProductAssistantService(
            RagService ragService,
            MemoryService memoryService,
            SessionManager sessionManager,
            PlannerService plannerService,
            ReflectionService reflectionService,
            RuntimeRunRecorder runRecorder,
            ProductConversationStore conversationStore
    ) {
        this.ragService = ragService;
        this.memoryService = memoryService;
        this.sessionManager = sessionManager;
        this.plannerService = plannerService;
        this.reflectionService = reflectionService;
        this.runRecorder = runRecorder;
        this.conversationStore = conversationStore;
    }

    public ProductAssistantService(
            RagService ragService,
            MemoryService memoryService,
            SessionManager sessionManager,
            PlannerService plannerService,
            ReflectionService reflectionService
    ) {
        this(ragService, memoryService, sessionManager, plannerService, reflectionService, null, new InMemoryProductConversationStore());
    }

    public ProductAssistantQueryResponse query(ProductAssistantQueryRequest request) {
        Instant startedAt = Instant.now();
        String requestId = runRecorder == null ? "product-" + UUID.randomUUID() : runRecorder.nextRunId();
        String rawConversationId = request.conversationId();
        String query = request.query() == null ? "" : request.query().trim();
        if (query.isBlank()) {
            throw new ProductApiException(400, "product_query_required", "query is required", rawConversationId, requestId, null);
        }
        if (query.length() > MAX_QUERY_LENGTH) {
            throw new ProductApiException(400, "product_query_too_long", "query is too long", rawConversationId, requestId, null);
        }
        if (rawConversationId != null && !rawConversationId.isBlank()) {
            if (rawConversationId.length() > MAX_CONVERSATION_ID_LENGTH) {
                throw new ProductApiException(400, "product_conversation_too_long", "conversationId is too long", rawConversationId, requestId, null);
            }
            if (!rawConversationId.matches(CONVERSATION_ID_PATTERN)) {
                throw new ProductApiException(400, "product_conversation_invalid", "conversationId contains unsupported characters", rawConversationId, requestId, null);
            }
        }
        if (request.topK() != null && (request.topK() < 1 || request.topK() > MAX_TOP_K)) {
            throw new ProductApiException(400, "product_topk_out_of_range", "topK must be between 1 and " + MAX_TOP_K, rawConversationId, requestId, null);
        }
        String conversationId = normalizeConversationId(rawConversationId);
        int topK = request.topK() == null ? DEFAULT_TOP_K : request.topK();
        try {
            ProductConversationState existingState = conversationStore.load(conversationId).orElse(null);
            boolean conversationCreated = existingState == null;

            SessionState session = sessionManager.session(conversationId);
            session.addUserMessage(query);

            List<RetrievalResult> retrieval = ragService.retrieve(query, topK);
            String answer = ragService.answer(query, retrieval);
            List<ProductSourceResponse> sources = retrieval.stream()
                    .map(ProductSourceResponse::from)
                    .toList();

            List<String> memoryHints = memoryService.relevantMemories(conversationId, query);
            ExecutionPlan plan = plannerService.plan(query);
            int planStepCount = plan.steps() == null ? 0 : plan.steps().size();
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
            signals.add("conversation:" + (conversationCreated ? "created" : "continued"));
            signals.add("rag:retrieved:" + retrieval.size());
            signals.add("memory:hints:" + memoryHints.size());
            signals.add("plan:steps:" + planStepCount);
            signals.add("reflection:" + (reflection.accepted() ? "accepted" : "needs-work"));
            signals.add("memory-write:" + memoryWriteDecision.name().toLowerCase());
            signals.add("conversation:stored");

            ProductConversationTurn turn = new ProductConversationTurn(
                    requestId,
                    startedAt,
                    query,
                    finalAnswer,
                    reflection.accepted() ? "COMPLETED" : "REJECTED",
                    retrieval.size(),
                    sources.size(),
                    retrieval.size(),
                    planStepCount,
                    List.copyOf(signals),
                    reflection.accepted() ? null : reflection.feedback()
            );

            ProductConversationState updatedState = new ProductConversationState(
                    conversationId,
                    existingState == null ? startedAt : existingState.createdAt(),
                    Instant.now(),
                    appendTurn(existingState, turn)
            );
            conversationStore.save(updatedState);

            ProductAssistantQueryResponse response = new ProductAssistantQueryResponse(
                    requestId,
                    conversationId,
                    conversationCreated,
                    session.size(),
                    updatedState.turnCount(),
                    query,
                    finalAnswer,
                    sources,
                    memoryHints,
                    new ProductPlanResponse(plan.formattedPlan(), plan.nextActiveStep() == null ? "" : plan.nextActiveStep().formattedLine(), planStepCount),
                    new ProductReflectionResponse(reflection.accepted(), reflection.feedback()),
                    List.copyOf(signals),
                    reflection.accepted() ? "COMPLETED" : "REJECTED",
                    reflection.accepted() ? null : reflection.feedback()
            );
            if (runRecorder != null) {
                runRecorder.recordProductRun(requestId, conversationId, query, response, startedAt, Instant.now());
            }
            return response;
        } catch (RuntimeException exception) {
            if (exception instanceof ProductApiException productException) {
                throw productException;
            }
            throw new ProductApiException(
                    503,
                    "product_pipeline_failed",
                    "Product query failed.",
                    conversationId,
                    requestId,
                    exception
            );
        }
    }

    private String normalizeConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "product-conv-" + UUID.randomUUID();
        }
        return conversationId.trim();
    }

    private List<ProductConversationTurn> appendTurn(ProductConversationState existingState, ProductConversationTurn turn) {
        List<ProductConversationTurn> turns = existingState == null ? new ArrayList<>() : new ArrayList<>(existingState.turns());
        turns.add(turn);
        return List.copyOf(turns);
    }
}
