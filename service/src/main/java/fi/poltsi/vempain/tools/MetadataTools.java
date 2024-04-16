package fi.poltsi.vempain.tools;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
public class MetadataTools {
	public void copyMetadata(File sourceFile, File destinationFile) {
		var builder = new ProcessBuilder();
		builder.command("exiftool", "-overwrite_original_in_place", "-TagsFromFile", sourceFile.getAbsolutePath(), "-all:all",
						destinationFile.getAbsolutePath());
		var output = new StringBuilder();

		try {
			var exitVal = runCommand(builder, output);

			if (exitVal == 0) {
				log.debug("Metadata copied successfully from file {} to file {}", sourceFile, destinationFile);
			} else {
				log.error("Failed to copy metadata from file {} to file {}. Exit value: {}\nOutput: {}", sourceFile, destinationFile,
						  exitVal, output);
			}
		} catch (IOException e) {
			log.error("Failed to copy metadata from file {} to file {}", sourceFile, destinationFile, e);
		} catch (InterruptedException e) {
			log.error("Failed to copy metadata from file {} to file {}", sourceFile, destinationFile, e);
			Thread.currentThread().interrupt();
		}
	}

	private int runCommand(ProcessBuilder builder, StringBuilder output) throws IOException, InterruptedException {
		Process        process        = builder.start();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

		String line;

		while ((line = bufferedReader.readLine()) != null) {
			output.append(line);
		}

		return process.waitFor();
	}

	public String getMetadataAsJSON(File file) {
		var builder = new ProcessBuilder();
		builder.command("exiftool", "-a", "-u", "-ee", "-api", "RequestAll=3", "-g1", "-J", file.getAbsolutePath());
		var output = new StringBuilder();

		try {
			var exitVal = runCommand(builder, output);

			if (exitVal == 0) {
				log.info("Metadata extracted successfully from file: {}", file);
			} else {
				log.error("Failed to extract metadata from file: {}", file);
			}

			return output.toString();
		} catch (IOException e) {
			log.error("Failed to parse metadata of file {}", file, e);
		} catch (InterruptedException e) {
			log.error("Failed to parse metadata of file {}", file, e);
			Thread.currentThread().interrupt();
		}

		return null;
	}
}
