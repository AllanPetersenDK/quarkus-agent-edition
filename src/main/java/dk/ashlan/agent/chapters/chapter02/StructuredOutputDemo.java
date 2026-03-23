package dk.ashlan.agent.chapters.chapter02;

import dk.ashlan.agent.llm.StructuredOutputParser;

public class StructuredOutputDemo {
    private final StructuredOutputParser parser = new StructuredOutputParser();

    public String run(String text) {
        return parser.parse(text).content();
    }
}
