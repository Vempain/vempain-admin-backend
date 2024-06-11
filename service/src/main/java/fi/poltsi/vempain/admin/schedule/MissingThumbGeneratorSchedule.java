package fi.poltsi.vempain.admin.schedule;

import fi.poltsi.vempain.admin.entity.file.FileImage;
import fi.poltsi.vempain.admin.entity.file.FileThumb;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.admin.service.file.FileThumbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Service
public class MissingThumbGeneratorSchedule {
	private static final long   DELAY         = 60 * 60 * 1000L;
	private static final String INITIAL_DELAY = "#{ 30 * 1000 + T(java.util.concurrent.ThreadLocalRandom).current().nextInt(" + DELAY + ") }";

	private final FileService      fileService;
	private final FileThumbService fileThumbService;

	@Value("${vempain.admin.file.converted-directory}")
	private String convertedDirectory;

	public MissingThumbGeneratorSchedule(FileService fileService, FileThumbService fileThumbService) {
		this.fileService      = fileService;
		this.fileThumbService = fileThumbService;
	}

	@Scheduled(fixedDelay = DELAY, initialDelayString = INITIAL_DELAY)
	public void findMissingThumbnails() {
		checkMissingInDatabase();
		checkMissingInFileSystem();
	}

	private void checkMissingInFileSystem() {
		var thumbs = fileService.findAllFileThumbs();

		if (thumbs != null && thumbs.iterator().hasNext()) {
			for (FileThumb fileThumb : thumbs) {
				var optionalFileCommon = fileService.findCommonById(fileThumb.getParentId());

				if (optionalFileCommon.isPresent()) {
					var fileCommon = optionalFileCommon.get();
					// Make sure the converted file exists
					if (!Files.exists(Paths.get(convertedDirectory + File.separator + fileCommon.getConvertedFile()))) {
						log.warn("Could not generate new thumb for parent ID {} because no converted file exists", fileCommon.getConvertedFile());
						continue;
					}

					// The thumb is created from the converted file, so it can't be empty
					if (fileCommon.getConvertedFile() != null && !fileCommon.getConvertedFile().isEmpty()) {
						var thumbFile = Paths.get(convertedDirectory + File.separator +
												  fileThumb.getFilepath() + File.separator +
												  fileThumb.getFilename());

						if (!Files.exists(thumbFile)) {
							log.info("Deleting the current thumb file entry in DB as it is missing in filesystem: {}", thumbFile);
							fileThumbService.delete(fileThumb.getId());
							log.info("Generating missing thumb file for: {}", fileThumb.getId());
							log.info("Generating thumb image for fileCommon: {}", fileCommon.getId());
							fileThumbService.generateThumbFile(fileCommon.getId());
						}
					} else {
						log.warn("Could not generate new thumb for parent ID {} because no app file exists", fileThumb.getParentId());
					}
				} else {
					log.warn("Could not find parent file for thumb with ID: {}", fileThumb.getParentId());
					// The thumb file entry should be deleted
				}
			}
		}
	}

	private void checkMissingInDatabase() {
		var images = fileService.findAllImagesWithoutThumbnail();

		if (images != null && images.iterator().hasNext()) {
			for (FileImage fileImage : images) {
				createThumbIfSourceFileExists(fileImage.getParentId());
			}
		}
	}

	private void createThumbIfSourceFileExists(long parentId) {
		var optionalFileCommon = fileService.findCommonById(parentId);

		if (optionalFileCommon.isPresent()) {
			var fileCommon = optionalFileCommon.get();
			// The thumb is created from the source file, so it can't be empty
			if (fileCommon.getConvertedFile() != null && !fileCommon.getConvertedFile().isEmpty()) {
				log.info("Generating thumb image for fileCommon: {}", parentId);
				fileThumbService.generateThumbFile(parentId);
			}
		}
	}
}
