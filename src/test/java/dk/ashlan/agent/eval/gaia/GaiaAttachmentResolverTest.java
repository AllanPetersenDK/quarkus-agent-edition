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
    void extractsTextAttachmentsAndAddsATraceEvents() throws Exception {
        Path snapshotRoot = tempDir.resolve("snapshot");
        Path validationDir = snapshotRoot.resolve("2023/validation");
        Files.createDirectories(validationDir);
        Path textFile = validationDir.resolve("sample.txt");
        Files.writeString(textFile, "Attachment content for the resolver test");

        GaiaAttachmentResolver resolver = new GaiaAttachmentResolver(path -> "This is the transcript from audio.");
        GaiaAttachment attachment = resolver.resolve(
                new GaiaExample("task-text", "What does the attachment say?", "1", "sample.txt", "2023/validation/sample.txt", "ignored", List.of("ignored"), java.util.Map.of(), null),
                snapshotRoot.toString()
        );

        assertEquals(GaiaAttachmentStatus.TEXT_EXTRACTED, attachment.status());
        assertTrue(attachment.present());
        assertTrue(attachment.supported());
        assertTrue(attachment.note().contains("Attachment content for the resolver test"));
        assertTrue(attachment.traceEvents().contains("attachment:text-extracted"));
    }

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

    @Test
    void reportsUnsupportedTypesExplicitly() throws Exception {
        Path snapshotRoot = tempDir.resolve("snapshot");
        Path validationDir = snapshotRoot.resolve("2023/validation");
        Files.createDirectories(validationDir);
        Path binaryFile = validationDir.resolve("sample.bin");
        Files.writeString(binaryFile, "binary-ish");

        GaiaAttachmentResolver resolver = new GaiaAttachmentResolver(path -> "This is the transcript from audio.");
        GaiaAttachment attachment = resolver.resolve(
                new GaiaExample("task-bin", "What is in the file?", "1", "sample.bin", "2023/validation/sample.bin", "ignored", List.of("ignored"), java.util.Map.of(), null),
                snapshotRoot.toString()
        );

        assertEquals(GaiaAttachmentStatus.UNSUPPORTED_TYPE, attachment.status());
        assertTrue(attachment.traceEvents().contains("attachment:unsupported-type"));
    }
}
