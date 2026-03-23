package dk.ashlan.agent.code;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CodeGenerationTool {
    public String generate(String request) {
        return "// Demo code generation placeholder\n// Request: " + request;
    }
}
