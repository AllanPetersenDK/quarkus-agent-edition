package dk.ashlan.agent.eval.gaia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HuggingFaceGaiaDatasetLoaderTest {
    @Test
    void failsClearlyWhenDatasetUrlIsMissing() {
        HuggingFaceGaiaDatasetLoader loader = new HuggingFaceGaiaDatasetLoader(new ObjectMapper(), "", "");

        IllegalStateException exception = assertThrows(IllegalStateException.class, loader::load);

        assertTrue(exception.getMessage().contains("GAIA dataset URL is required"));
    }
}
