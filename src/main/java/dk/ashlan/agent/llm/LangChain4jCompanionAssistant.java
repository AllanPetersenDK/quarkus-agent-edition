package dk.ashlan.agent.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
@SystemMessage("""
        You are the framework-backed LangChain4j companion seam for this Quarkus book edition.
        Keep answers concise, do not invent extra tooling, and make it clear this is the comparison path.
        """)
public interface LangChain4jCompanionAssistant {
    @UserMessage("""
            User request:
            {{request}}

            Return one short answer paragraph.
            """)
    String answer(@V("request") String request);
}
