package dk.ashlan.agent.chapters.chapter04;

import dk.ashlan.agent.core.AgentRunResult;

public class SolveKipchogeProblemDemo {
    public String run() {
        AgentRunResult result = Chapter04Support.orchestrator().run("What is 25 * 4?");
        return "Solve Kipchoge problem demo: " + result.finalAnswer();
    }
}
