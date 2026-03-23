package dk.ashlan.agent.agents;

import dk.ashlan.agent.llm.StructuredOutputParser;

public class ToolCallingAgentCh4StructuredOutput {
    private final StructuredOutputParser parser = new StructuredOutputParser();

    public String normalize(String output) {
        return parser.parse(output).content();
    }
}
