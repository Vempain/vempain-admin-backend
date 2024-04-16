package fi.poltsi.vempain.admin.schedule;

import fi.poltsi.vempain.admin.entity.file.FileCommon;
import fi.poltsi.vempain.admin.entity.file.FileImage;
import fi.poltsi.vempain.admin.entity.file.FileThumb;
import fi.poltsi.vempain.admin.service.file.FileService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

	private ArrayList<ArrayList<FileImage>> duplicateImages;

	@Scheduled(fixedDelay = DELAY, initialDelayString = INITIAL_DELAY)
	public void removeDuplicateImages() {
		var iterableCommonFiles = fileService.getDuplicateCommonFiles();
		var iterableThumbFiles = fileService.getDuplicateThumbFiles();

		cleanFileCommons(iterableCommonFiles);
		cleanThumbFiles(iterableThumbFiles);
	}

	private void cleanThumbFiles(Iterable<FileThumb> iterableThumbFiles) {
		for (FileThumb fileThumb : iterableThumbFiles) {
			deduplicateThumbFile(fileThumb);
		}
	}

	private void cleanFileCommons(List<FileCommon> iterableFileCommons) {
		for (FileCommon fileCommon : iterableFileCommons) {
			deduplicateCommonFile(fileCommon);
		}
	}

	private void deduplicateThumbFile(FileThumb fileThumb) {
		Iterable<FileThumb> fileThumbs = fileService.findAllFileThumbsByFilepathFilename(fileThumb.getFilepath(), fileThumb.getFilename());
		var counter = 0;
		ArrayList<FileThumb> duplicates = new ArrayList<>();
		// Check if the parent file is present in file_common

		for (FileThumb iteratingThumb : fileThumbs) {
			var optionalFileCommon = fileService.findCommonById(iteratingThumb.getParentId());
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
	}

	private void deduplicateCommonFile(FileCommon fileCommon) {
		Iterable<FileImage> fileImages = fileService.findAllFileImagesByFilepathFilename(fileCommon.getSiteFilepath(), fileCommon.getSiteFilename());
		var counter = 0;
		ArrayList<FileImage> duplicates = new ArrayList<>();
		// Check if the parent file is present in file_common

		for (FileImage iteratingImage : fileImages) {
			var optionalFileCommon = fileService.findCommonById(iteratingImage.getParentId());
			if (optionalFileCommon.isEmpty()) {
				log.info("Removing thumb file {} because it does not refer to valid common file", iteratingImage);
				fileService.deleteFileImage(iteratingImage);
			} else {
				if (counter > 0){
					log.info("Found a duplicate image {} with valid common: {}", iteratingImage, optionalFileCommon);
				}

				counter++;
				duplicates.add(iteratingImage);
			}
		}

		if (counter > 1) {
			duplicateImages.add(duplicates);
		}
	}
}
