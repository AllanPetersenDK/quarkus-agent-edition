package dk.ashlan.agent.chapters.chapter08;

public class WorkspaceRoundTripDemo {
    public String run() {
        return Chapter08Support.writeAndRead("demo/round-trip.txt", "hello workspace");
    }
}
