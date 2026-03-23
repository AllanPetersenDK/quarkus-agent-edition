package dk.ashlan.agent.chapters.chapter03;

public class ToolsExerciseDemo {
    public String run() {
        String calculator = Chapter03Support.calculatorOutput();
        String time = Chapter03Support.executor().execute("clock", java.util.Map.of()).output();
        return "Tools exercise demo: " + calculator + " | " + time;
    }
}
