package dk.ashlan.agent.chapters.chapter08;

public class CodeAgentDemo {
    public String run(String request) {
        return Chapter08Support.orchestrator().run(request);
    }
}
