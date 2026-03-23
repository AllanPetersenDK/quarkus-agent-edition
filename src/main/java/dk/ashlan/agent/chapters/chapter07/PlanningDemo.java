package dk.ashlan.agent.chapters.chapter07;

import dk.ashlan.agent.planning.ExecutionPlan;

public class PlanningDemo {
    public ExecutionPlan run(String goal) {
        return Chapter07Support.plan(goal);
    }
}
