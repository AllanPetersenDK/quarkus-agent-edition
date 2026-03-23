package dk.ashlan.agent.chapters.chapter02;

import dk.ashlan.agent.llm.LlmCompletion;

public class PotatoProblemDemo {
    public String solve(String rawOutput) {
        LlmCompletion completion = Chapter02Support.parser().parse(rawOutput);
        if (rawOutput != null && rawOutput.trim().startsWith("answer:")
                && completion.content() != null && !completion.content().isBlank()) {
            return completion.content();
        }
        return Chapter02Support.parser().parse("answer: potato validated").content();
    }
}
