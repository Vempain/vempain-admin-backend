package fi.poltsi.vempain.admin.configuration;

import fi.poltsi.vempain.admin.api.FileClassEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class StorageDirectoryConfiguration {

	@Value("${vempain.admin.file.site-file-directory}")
	private String siteFileStorage;

	@Bean
	public Map<String, String> storageLocations() {
		HashMap<String, String> storageLocations = new HashMap<>();
		var                     exceptionMessage = "Unable to generate storage location map";

		for (String fileClassName : FileClassEnum.getFileClassNames()) {
			var tmpPath = siteFileStorage + File.separator + fileClassName;
			log.info("Initializing, adding storage location {}: {}", fileClassName, tmpPath);
			storageLocations.put(fileClassName, tmpPath);
			var tmpDir = new File(tmpPath);

			if (!tmpDir.exists() && !tmpDir.mkdirs()) {
				log.error("Type {} file storage did not exist and it could not be created as: {}", fileClassName, tmpDir);
				throw new FileSystemNotFoundException(exceptionMessage);
			} else if (!Files.isReadable(tmpDir.toPath())) {
				log.error("Type {} file storage has wrong permission: {}", fileClassName, tmpDir);
				throw new FileSystemNotFoundException(exceptionMessage);
			}
		}

		return storageLocations;
	}
}
