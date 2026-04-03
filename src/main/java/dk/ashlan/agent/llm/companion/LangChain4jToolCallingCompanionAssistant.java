package dk.ashlan.agent.llm.companion;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(tools = LangChain4jToolCallingCompanionTools.class)
@SystemMessage("""
        You are the framework-backed LangChain4j tool-calling companion seam for this Quarkus book edition.
        Use the calculator and clock tools when helpful, keep answers concise, and make it clear this is the comparison path.
        """)
public interface LangChain4jToolCallingCompanionAssistant {
    @UserMessage("""
            User request:
            {{request}}

            Use the available tools only when they help answer the request.
            Return one short answer paragraph.
            """)
    String answer(@V("request") String request);
}
