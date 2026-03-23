package dk.ashlan.agent.chapters.chapter07;

import dk.ashlan.agent.core.AgentRunResult;

public class ImprovementLoopDemo {
    public String run(String goal) {
        AgentRunResult result = Chapter07Support.orchestrator().run(goal);
        return result.finalAnswer() + " | " + result.stopReason();
    }
}
