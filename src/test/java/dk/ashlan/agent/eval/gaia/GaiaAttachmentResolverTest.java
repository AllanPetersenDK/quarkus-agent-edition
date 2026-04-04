package dk.ashlan.agent.eval.gaia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GaiaAttachmentResolverTest {
    @TempDir
    Path tempDir;

    @Test
    void transcribesAudioAttachmentsAndAddsATraceEvent() throws Exception {
        Path snapshotRoot = tempDir.resolve("snapshot");
        Path validationDir = snapshotRoot.resolve("2023/validation");
        Files.createDirectories(validationDir);
        Path audioFile = validationDir.resolve("sample.mp3");
        Files.writeString(audioFile, "not real audio, but good enough for the resolver test");

        GaiaAttachmentResolver resolver = new GaiaAttachmentResolver(path -> "This is the transcript from audio.");
        GaiaAttachment attachment = resolver.resolve(
                new GaiaExample("task-audio", "What does the audio say?", "1", "sample.mp3", "2023/validation/sample.mp3", "ignored", List.of("ignored"), java.util.Map.of(), null),
                snapshotRoot.toString()
        );

        assertEquals(GaiaAttachmentStatus.AUDIO_TRANSCRIBED, attachment.status());
        assertTrue(attachment.present());
        assertTrue(attachment.supported());
        assertTrue(attachment.note().contains("This is the transcript from audio."));
        assertTrue(attachment.traceEvents().contains("attachment:audio-transcribed"));
    }

    @Test
    void reportsAudioTranscriptionFailuresExplicitly() throws Exception {
        Path snapshotRoot = tempDir.resolve("snapshot");
        Path validationDir = snapshotRoot.resolve("2023/validation");
        Files.createDirectories(validationDir);
        Path audioFile = validationDir.resolve("sample.mp3");
        Files.writeString(audioFile, "not real audio, but good enough for the resolver test");

        GaiaAttachmentResolver resolver = new GaiaAttachmentResolver(path -> {
            throw new IllegalStateException("transcription backend unavailable");
        });
        GaiaAttachment attachment = resolver.resolve(
                new GaiaExample("task-audio-fail", "What does the audio say?", "1", "sample.mp3", "2023/validation/sample.mp3", "ignored", List.of("ignored"), java.util.Map.of(), null),
                snapshotRoot.toString()
        );

        assertEquals(GaiaAttachmentStatus.AUDIO_TRANSCRIPTION_FAILED, attachment.status());
        assertTrue(attachment.traceEvents().contains("attachment:audio-transcription-failed"));
        assertTrue(attachment.note().contains("transcription backend unavailable"));
    }
}
