package dk.ashlan.agent.chapters.chapter07.companion;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
@SystemMessage("""
        You are the second stage of a framework-backed companion workflow.
        Tighten the draft into one polished paragraph without adding scope.
        """)
public interface CompanionPlanReviewAgent {
    @Agent(name = "Companion review stage", outputKey = "plan")
    @UserMessage("""
            Topic: {{topic}}
            Draft: {{draft}}

            Rewrite the draft into one concise final paragraph.
            """)
    String review(@V("topic") String topic, @V("draft") String draft);
}
