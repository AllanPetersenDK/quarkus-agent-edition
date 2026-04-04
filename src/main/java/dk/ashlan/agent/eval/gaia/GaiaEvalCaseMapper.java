package dk.ashlan.agent.eval.gaia;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class GaiaEvalCaseMapper {
    public GaiaEvalCase map(GaiaExample example) {
        GaiaAttachment attachment = example.attachment();
        return new GaiaEvalCase(
                example.taskId(),
                example.question(),
                List.copyOf(example.expectedAnswers()),
                example.level(),
                example.filePath(),
                attachment,
                example.rawMetadata()
        );
    }
}
