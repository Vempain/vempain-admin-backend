package fi.poltsi.vempain.admin.schedule;

import fi.poltsi.vempain.admin.entity.file.FileThumb;
import fi.poltsi.vempain.admin.service.file.FileService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Clean up duplicates from file_image and file_thumb tables. See example:
 * SELECT ft.filename
 * , ft.filepath
 * , COUNT(ft.id) AS rowCount
 * FROM file_thumb ft
 * GROUP BY ft.filename, ft.filepath
 * ORDER BY rowCount DESC;
 */

@Slf4j
@AllArgsConstructor
@Service
public class DuplicateFileRemovalSchedule {
	private static final long   DELAY         = 60 * 60 * 1000L;
	private static final String INITIAL_DELAY = "#{ 30 * 1000 + T(java.util.concurrent.ThreadLocalRandom).current().nextInt(" + DELAY + ") }";

	private final FileService fileService;

	@Scheduled(fixedDelay = DELAY, initialDelayString = INITIAL_DELAY)
	public void removeDuplicateImages() {
		var iterableThumbFiles = fileService.getDuplicateThumbFiles();
		cleanThumbFiles(iterableThumbFiles);
	}

	private void cleanThumbFiles(Iterable<FileThumb> iterableThumbFiles) {
		for (FileThumb fileThumb : iterableThumbFiles) {
			deduplicateThumbFile(fileThumb);
		}
	}

	private void deduplicateThumbFile(FileThumb fileThumb) {
		Iterable<FileThumb> fileThumbs = fileService.findAllFileThumbsByFilepathFilename(fileThumb.getFilepath(), fileThumb.getFilename());
		var counter = 0;
		ArrayList<FileThumb> duplicates = new ArrayList<>();
		// Check if the parent file is present in file_common

		for (FileThumb iteratingThumb : fileThumbs) {
			var optionalFileCommon = fileService.findSiteFileById(iteratingThumb.getParentId());

			if (optionalFileCommon.isEmpty()) {
				log.info("Removing thumb file {} because it does not refer to valid common file", iteratingThumb);
				fileService.deleteFileThumb(iteratingThumb);
			} else {
				if (counter > 0){
					log.info("Found a duplicate image {} with valid common: {}", iteratingThumb, optionalFileCommon);
				}

				counter++;
				duplicates.add(iteratingThumb);
			}
		}

		// TODO Remove the duplicates
		log.info("Found {} duplicates for thumb file {}", duplicates.size(), fileThumb);
	}
}
