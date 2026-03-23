package dk.ashlan.agent.chapters.chapter04;

import dk.ashlan.agent.llm.LlmCompletion;

public class AgentStructuredOutputDemo {
    public String run() {
        LlmCompletion completion = Chapter04Support.structuredOutputOrchestrator().parse("answer: Agent structured output demo");
        return completion.content();
    }
}
