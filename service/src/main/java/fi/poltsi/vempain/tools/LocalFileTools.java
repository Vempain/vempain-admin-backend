package fi.poltsi.vempain.tools;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Slf4j
@UtilityClass
public class LocalFileTools {
	private static final String RESPONSE_STATUS_EXCEPTION_MESSAGE = "Unknown error";

	public static void createAndVerifyDirectory(Path directory) {
		if (!Files.exists(directory)) {
			log.warn("Path {} does not exist, attempting to create it", directory);

			try {
				Files.createDirectories(directory);
			} catch (IOException e) {
				log.error("Failed to create the target directory {}", directory, e);
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, RESPONSE_STATUS_EXCEPTION_MESSAGE);
			}
		}
	}

	public static String computeSha256(File file) {
		try {
			return DigestUtils.sha256Hex(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
			return null;
		}
	}

	public static long getFileSize(Path filepath) {
		try {
			return Files.size(filepath);
		} catch (IOException e) {
			log.error("Failed to retrieve file size for file: {}", filepath);
			return 0L;
		}
	}

	public static String setExtension(String filename, String extension) {
		int pos = filename.lastIndexOf('.');
		return filename.substring(0, pos) + "." + extension;
	}

	/**
	 * Recursively remove a directory and all its contents
	 *
	 * @param directory Main directory to be removed including all its children
	 */
	public static void removeDirectory(String directory) {
		try {
			Files.walk(Path.of(directory))
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		} catch (IOException e) {
			log.error("Failed to remove directory {}", directory, e);
		}
	}
}
