package fi.poltsi.vempain.admin.schedule;

import fi.poltsi.vempain.admin.api.request.FileProcessRequest;
import fi.poltsi.vempain.admin.repository.file.ScanQueueScheduleRepository;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@AllArgsConstructor
@Service
public class DirectoryImportSchedule {
	private static final long                             DELAY         = 5 * 60 * 1000L;
	private static final String                           INITIAL_DELAY =
			"#{ 5 * 1000 + T(java.util.concurrent.ThreadLocalRandom).current().nextInt(" + DELAY + ") }";

	private final FileService fileService;
	private final ScanQueueScheduleRepository scanQueueScheduleRepository;

	@Scheduled(fixedDelay = DELAY, initialDelayString = INITIAL_DELAY)
	public void importFilesFromDirectory() {
		var scheduledImports = scanQueueScheduleRepository.findAll();

		if (scheduledImports.isEmpty()) {
			log.info("No scheduled imports found");
			return;
		}

		for (var scheduledImport : scheduledImports) {
			log.info("Importing files from directory: {}", scheduledImport.getSourceDirectory());
			var request = FileProcessRequest.builder()
											.sourceDirectory(scheduledImport.getSourceDirectory())
											.destinationDirectory(scheduledImport.getDestinationDirectory())
											.generateGallery(scheduledImport.isCreateGallery())
											.galleryShortname(scheduledImport.getGalleryShortname())
											.galleryDescription(scheduledImport.getGalleryDescription())
											.generatePage(scheduledImport.isCreatePage())
											.pageTitle(scheduledImport.getPageTitle())
											.pagePath(scheduledImport.getPagePath())
											.pageBody(scheduledImport.getPageBody())
											.pageFormId(scheduledImport.getPageFormId())
											.build();
			try {
				fileService.addFilesFromDirectory(request, scheduledImport.getCreatedBy());
			} catch (VempainEntityNotFoundException e) {
				log.error("The import directory no longer exists: {}", scheduledImport.getSourceDirectory());
			} catch (IOException e) {
				log.error("Error importing files from directory: {}", scheduledImport.getSourceDirectory(), e);
			} catch (VempainAclException e) {
				log.error("ACL error importing files from directory: {}", scheduledImport.getSourceDirectory(), e);
			} finally {
				scanQueueScheduleRepository.delete(scheduledImport);
			}
		}
	}
}
