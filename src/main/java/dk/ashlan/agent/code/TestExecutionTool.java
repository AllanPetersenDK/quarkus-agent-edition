package dk.ashlan.agent.code;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TestExecutionTool {
    public CommandResult runTests() {
        return new CommandResult(0, "Test execution is a placeholder in the companion edition.", "");
    }
}
