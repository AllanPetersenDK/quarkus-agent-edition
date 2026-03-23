package dk.ashlan.agent.planning;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReflectionService {
    public ReflectionResult reflect(String output) {
        if (output == null || output.length() < 20) {
            return ReflectionResult.rejected("Output is too thin for the chapter goal.", output == null ? "" : output + " Please expand the answer.");
        }
        return ReflectionResult.accepted(output);
    }
}
