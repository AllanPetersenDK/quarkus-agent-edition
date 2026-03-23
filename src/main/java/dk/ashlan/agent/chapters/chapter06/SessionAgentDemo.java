package dk.ashlan.agent.chapters.chapter06;

import dk.ashlan.agent.sessions.Session;

import java.util.List;

public class SessionAgentDemo {
    public List<String> run() {
        Session session = Chapter06Support.sessions().getOrCreate("chapter-06");
        session.addEvent("hello");
        session.addEvent("still here");
        return session.events();
    }
}
