package fi.poltsi.vempain.admin.schedule;

import fi.poltsi.vempain.admin.service.file.FileService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@AllArgsConstructor
@Service
public class SubjectFileConsistencySchedule {
	private static final long   DELAY         = 60 * 60 * 1000L;
	private static final String INITIAL_DELAY = "#{ 30 * 1000 + T(java.util.concurrent.ThreadLocalRandom).current().nextInt(" + DELAY + ") }";

	private final FileService fileService;

	@Scheduled(fixedDelay = DELAY, initialDelayString = INITIAL_DELAY)
	public void verify() {
		removeOrphanedJoins();
	}

	private void removeOrphanedJoins() {
		// Get set of file IDs from file_common
		var siteFileIdSet = getSiteFileIdSet();
		// Get set of file IDs from file_subject
		Set<Long> fileSubjectIdSet = fileService.findAllSiteFileIdWithSubject();

		// We're interested in the file IDs which are listed in the file_subject-table, but which are missing from file_common table. So
		// we remove all fileCommonIdSet from fileSubjectIdSet
		fileSubjectIdSet.removeAll(siteFileIdSet);

		if (fileSubjectIdSet.isEmpty()) {
			log.debug("No orphaned file_subject entries found");
		} else {
			log.info("Found {} file_subject entries that do not have a site file and needs to be purged", fileSubjectIdSet.size());
			// We need to purge the file_subject-table
			fileService.removeFileSubjects(fileSubjectIdSet);
		}
	}

	private Set<Long> getSiteFileIdSet() {
		var siteFiles = fileService.findAllSiteFiles();
		Set<Long> resultSet = new HashSet<>();

		for (var fileCommon : siteFiles) {
			resultSet.add(fileCommon.getId());
		}

		return resultSet;
	}
}
