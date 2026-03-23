package dk.ashlan.agent.chapters.chapter04;

import dk.ashlan.agent.core.AgentRunResult;

public class HumanInTheLoopDemo {
    public String run() {
        var orchestrator = Chapter04Support.callbackOrchestrator();
        AgentRunResult result = orchestrator.run("Solve 25 * 4");
        return "Human-in-the-loop demo: " + String.join(", ", orchestrator.callbacks()) + " -> " + result.finalAnswer();
    }
}
