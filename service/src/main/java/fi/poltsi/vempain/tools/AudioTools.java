package fi.poltsi.vempain.tools;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

public class AudioTools {
	public static long getAudioLength(File audioFile) {
		AudioInputStream audioInputStream  = null;
		try {
			audioInputStream = AudioSystem.getAudioInputStream(audioFile);
		} catch (UnsupportedAudioFileException | IOException e) {
			return 0L;
		}

		AudioFormat      format            = audioInputStream.getFormat();
		long             audioFileLength   = audioFile.length();
		int              frameSize         = format.getFrameSize();
		float            frameRate         = format.getFrameRate();
		float            durationInSeconds = (audioFileLength / (frameSize * frameRate));
		// Convert float to long
		return (long) durationInSeconds;
	}
}
