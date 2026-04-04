package dk.ashlan.agent.tools;

public record ToolDefinition(
        String name,
        String description,
        boolean requiresConfirmation,
        String confirmationMessageTemplate
) {
    public ToolDefinition(String name, String description) {
        this(name, description, false, null);
    }

    public ToolDefinition {
        if (confirmationMessageTemplate == null || confirmationMessageTemplate.isBlank()) {
            confirmationMessageTemplate = "Approve tool " + name + "?";
        }
    }
}
