package dk.ashlan.agent.chapters.chapter07.companion;

import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface CompanionPlanWorkflow {
    @SequenceAgent(
            outputKey = "plan",
            subAgents = {CompanionPlanDraftAgent.class, CompanionPlanReviewAgent.class}
    )
    String run(@V("topic") String topic);
}
