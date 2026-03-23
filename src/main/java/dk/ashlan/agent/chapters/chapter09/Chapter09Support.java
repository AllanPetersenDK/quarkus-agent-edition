package dk.ashlan.agent.chapters.chapter09;

import dk.ashlan.agent.multiagent.AgentRouter;
import dk.ashlan.agent.multiagent.AgentTaskResult;
import dk.ashlan.agent.multiagent.CodingAgent;
import dk.ashlan.agent.multiagent.CoordinatorAgent;
import dk.ashlan.agent.multiagent.ResearchAgent;
import dk.ashlan.agent.multiagent.ReviewerAgent;

import java.util.List;

final class Chapter09Support {
    private Chapter09Support() {
    }

    static CoordinatorAgent coordinator() {
        return new CoordinatorAgent(
                new AgentRouter(List.of(new ResearchAgent(), new CodingAgent())),
                new ReviewerAgent()
        );
    }

    static AgentTaskResult researchFlow() {
        return coordinator().run("research the Quarkus agent edition");
    }

    static AgentTaskResult codingFlow() {
        return coordinator().run("implement the agent routing example");
    }
}
