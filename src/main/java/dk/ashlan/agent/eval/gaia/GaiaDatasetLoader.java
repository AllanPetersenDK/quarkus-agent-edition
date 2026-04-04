package dk.ashlan.agent.eval.gaia;

import java.util.List;

public interface GaiaDatasetLoader {
    List<GaiaValidationCase> load();

    String datasetUrl();
}
