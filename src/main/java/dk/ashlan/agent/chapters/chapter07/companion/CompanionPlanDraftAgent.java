package dk.ashlan.agent.chapters.chapter07.companion;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
@SystemMessage("""
        You are the first stage of a framework-backed companion workflow.
        Draft a short, practical outline only.
        """)
public interface CompanionPlanDraftAgent {
    @Agent(name = "Companion draft stage", outputKey = "draft")
    @UserMessage("""
            Topic: {{topic}}
            Produce a compact first draft with no more than two bullet points.
            """)
    String draft(@V("topic") String topic);
}
