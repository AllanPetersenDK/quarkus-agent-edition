package dk.ashlan.agent.chapters.chapter02;

import dk.ashlan.agent.llm.LlmResponse;

public class LlmChatDemo {
    public LlmResponse run(String message) {
        return LlmResponse.answer("Chat demo: " + message);
    }

    public static void main(String[] args) {
        System.out.println(new LlmChatDemo().run(args.length == 0 ? "hello" : args[0]).content());
    }
}
