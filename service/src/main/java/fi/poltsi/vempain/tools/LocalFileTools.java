package fi.poltsi.vempain.tools;

import jakarta.xml.bind.annotation.adapters.HexBinaryAdapter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

	public static String getSha1OfFile(File file) {
		MessageDigest sha1;

		try {
			sha1 = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			log.error("Could not find message digest SHA-1");
			return null;
		}

		try (InputStream input = new FileInputStream(file)) {

			byte[] buffer = new byte[8192];
			int    len    = input.read(buffer);

			while (len != -1) {
				sha1.update(buffer, 0, len);
				len = input.read(buffer);
			}

			return new HexBinaryAdapter().marshal(sha1.digest());
		} catch (FileNotFoundException e) {
			log.error("Failed to read file {}", file);
		} catch (IOException e) {
			log.error("Failed to read file {}", file, e);
		}

		return null;
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
