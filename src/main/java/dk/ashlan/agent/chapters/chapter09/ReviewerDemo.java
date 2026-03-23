package dk.ashlan.agent.chapters.chapter09;

import dk.ashlan.agent.multiagent.AgentTask;
import dk.ashlan.agent.multiagent.ReviewerAgent;

public class ReviewerDemo {
    public String run(String output) {
        return new ReviewerAgent().execute(new AgentTask("review-1", "review output", output)).review();
    }
}
