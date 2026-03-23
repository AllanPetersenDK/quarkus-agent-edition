package dk.ashlan.agent.code;

public record CommandResult(int exitCode, String output, String error) {
}
