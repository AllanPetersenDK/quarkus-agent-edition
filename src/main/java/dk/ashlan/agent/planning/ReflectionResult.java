package dk.ashlan.agent.planning;

public record ReflectionResult(boolean accepted, String feedback, String revisedOutput) {
    public static ReflectionResult accepted(String output) {
        return new ReflectionResult(true, "", output);
    }

    public static ReflectionResult rejected(String feedback, String revisedOutput) {
        return new ReflectionResult(false, feedback, revisedOutput);
    }
}
