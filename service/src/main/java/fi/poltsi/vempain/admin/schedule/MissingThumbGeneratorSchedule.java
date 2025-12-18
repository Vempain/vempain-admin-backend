package fi.poltsi.vempain.admin.schedule;

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

	@Value("${vempain.admin.file.site-file-directory}")
	private String siteFileDirectory;

	public MissingThumbGeneratorSchedule(FileService fileService, FileThumbService fileThumbService) {
		this.fileService = fileService;
		this.fileThumbService = fileThumbService;
	}

	@Scheduled(fixedDelay = DELAY, initialDelayString = INITIAL_DELAY)
	public void findMissingThumbnails() {
		checkMissingInFileSystem();
	}

	private void checkMissingInFileSystem() {
		var thumbs = fileService.findAllFileThumbs();

		if (thumbs != null && thumbs.iterator()
									.hasNext()) {
			for (FileThumb fileThumb : thumbs) {
				var optionalSiteFile = fileService.findSiteFileById(fileThumb.getParentId());

				if (optionalSiteFile.isPresent()) {
					var siteFile = optionalSiteFile.get();
					// Make sure the converted file exists
					var checkFile =
							Paths.get(siteFileDirectory + File.separator + siteFile.getFileType().shortName + File.separator + siteFile.getFilePath() + File.separator + siteFile.getFileName());
					log.debug("Checking existence of file for thumb generation: {}", checkFile);

					if (!Files.exists(checkFile)) {
						log.warn("Could not generate new thumb for parent ID {} because no site file exists", checkFile);
						continue;
					}

					// The thumb is created from the converted file, so it can't be empty
					if (siteFile.getFileName() != null && !siteFile.getFileName()
																   .isEmpty()) {
						var thumbFile = Paths.get(siteFileDirectory + File.separator +
												  fileThumb.getFilepath() + File.separator +
												  fileThumb.getFilename());

						if (!Files.exists(thumbFile)) {
							log.info("Deleting the current thumb file entry in DB as it is missing in filesystem: {}", thumbFile);
							fileThumbService.delete(fileThumb.getId());
							log.info("Generating missing thumb file for: {}", fileThumb.getId());
							log.info("Generating thumb image for site file: {}", siteFile.getId());
							fileThumbService.generateThumbFile(siteFile.getId());
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
}
