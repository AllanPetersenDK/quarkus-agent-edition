package dk.ashlan.agent.api;

import dk.ashlan.agent.code.CodeAgentOrchestrator;
import dk.ashlan.agent.code.CommandResult;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/code-agent")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CodeAgentResource {
    private final CodeAgentOrchestrator orchestrator;

    public CodeAgentResource(CodeAgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @POST
    public Map<String, Object> run(Map<String, String> input) {
        String message = input.getOrDefault("message", "");
        CommandResult result = orchestrator.runTests();
        return Map.of(
                "response", orchestrator.run(message),
                "testExitCode", result.exitCode(),
                "testOutput", result.output()
        );
    }
}
