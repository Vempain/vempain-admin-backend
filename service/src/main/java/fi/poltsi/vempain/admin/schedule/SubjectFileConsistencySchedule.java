package fi.poltsi.vempain.admin.schedule;

import fi.poltsi.vempain.admin.entity.file.FileCommon;
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
		Set<Long> fileCommonIdSet = getFileCommonIdSet();
		// Get set of file IDs from file_subject
		Set<Long> fileSubjectIdSet = fileService.findAllFileCommonIdWithSubject();

		// We're interested in the file IDs which are listed in the file_subject-table, but which are missing from file_common table. So
		// we remove all fileCommonIdSet from fileSubjectIdSet
		fileSubjectIdSet.removeAll(fileCommonIdSet);

		if (fileSubjectIdSet.isEmpty()) {
			log.info("No orphaned file_subject entries found");
		} else {
			log.info("Found {} file_subject entries that do not have a file_common file and needs to be purged", fileSubjectIdSet.size());
			// We need to purge the file_subject-table
			fileService.removeFileSubjects(fileSubjectIdSet);
		}
	}

	private Set<Long> getFileCommonIdSet() {
		Iterable<FileCommon> fileCommonIterable = fileService.findAllFileCommon();
		Set<Long> resultSet = new HashSet<>();

		for (FileCommon fileCommon : fileCommonIterable) {
			resultSet.add(fileCommon.getId());
		}

		return resultSet;
	}
}
