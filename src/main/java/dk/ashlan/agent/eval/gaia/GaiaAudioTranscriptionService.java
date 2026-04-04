package dk.ashlan.agent.eval.gaia;

import java.nio.file.Path;

@FunctionalInterface
public interface GaiaAudioTranscriptionService {
    String transcribe(Path audioFile);
}
