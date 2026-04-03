package dk.ashlan.agent.chapters.chapter07.companion;

import dk.ashlan.agent.llm.companion.LangChain4jToolCallingCompanionAssistant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class LangChain4jToolCallingCompanionDemo {
    private final LangChain4jToolCallingCompanionAssistant assistant;

    @Inject
    public LangChain4jToolCallingCompanionDemo(Instance<LangChain4jToolCallingCompanionAssistant> assistants) {
        this(assistants.isResolvable() ? assistants.get() : null);
    }

    LangChain4jToolCallingCompanionDemo(LangChain4jToolCallingCompanionAssistant assistant) {
        this.assistant = assistant;
    }

    public String run(String request) {
        if (assistant == null) {
            return "Framework-backed tool-calling companion demo is not configured for: " + request;
        }
        return assistant.answer(request);
    }
}
