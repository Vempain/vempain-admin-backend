package fi.poltsi.vempain.tools;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AudioToolsUTC {

    @TempDir
    Path tempDir;

    @Test
    void lengthOfAWaveFileIsComputedFromItsFrameRateAndSize() throws Exception {
        var format = new AudioFormat(8000f, 16, 1, true, false);
        var seconds = 3;
        var samples = new byte[seconds * 8000 * format.getFrameSize()];
        var wav = tempDir.resolve("tone.wav")
            .toFile();

        try (var stream = new AudioInputStream(new ByteArrayInputStream(samples), format, samples.length / format.getFrameSize())) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, wav);
        }

        // The WAVE header adds a few bytes, which round away when the float duration is truncated to seconds
        assertEquals(seconds, AudioTools.getAudioLength(wav));
    }

    @Test
    void unsupportedOrMissingFilesHaveZeroLength() throws Exception {
        var text = tempDir.resolve("notes.txt");
        Files.writeString(text, "not audio at all");

        assertEquals(0L, AudioTools.getAudioLength(text.toFile()));
        assertEquals(0L, AudioTools.getAudioLength(tempDir.resolve("missing.wav")
            .toFile()));
    }

    @Test
    void theUtilityCanBeInstantiated() {
        assertNotNull(new AudioTools());
    }
}
