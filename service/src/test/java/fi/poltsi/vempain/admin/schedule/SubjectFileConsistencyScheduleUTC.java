package fi.poltsi.vempain.admin.schedule;

import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.service.file.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Subject links that refer to a site file which no longer exists are purged; links of existing files are left alone.
 */
@ExtendWith(MockitoExtension.class)
class SubjectFileConsistencyScheduleUTC {
	@Mock
	private FileService fileService;

	@InjectMocks
	private SubjectFileConsistencySchedule schedule;

	private static List<SiteFile> siteFiles(long... ids) {
		return java.util.Arrays.stream(ids)
							   .mapToObj(id -> (SiteFile) SiteFile.builder()
																  .id(id)
																  .build())
							   .toList();
	}

	@Test
	void nothingHappensWhenSchedulingIsDisabled() {
		ReflectionTestUtils.setField(schedule, "schedulingEnabled", false);

		schedule.verify();

		verifyNoInteractions(fileService);
	}

	@Test
	void nothingIsPurgedWhenEverySubjectLinkHasASiteFile() {
		when(fileService.findAllSiteFiles()).thenReturn(siteFiles(1L, 2L, 3L));
		when(fileService.findAllSiteFileIdWithSubject()).thenReturn(new HashSet<>(Set.of(1L, 3L)));

		schedule.verify();

		verify(fileService, never()).removeFileSubjects(any());
	}

	@Test
	void subjectLinksOfMissingSiteFilesArePurged() {
		when(fileService.findAllSiteFiles()).thenReturn(siteFiles(1L, 2L));
		when(fileService.findAllSiteFileIdWithSubject()).thenReturn(new HashSet<>(Set.of(1L, 7L, 8L)));

		schedule.verify();

		verify(fileService).removeFileSubjects(Set.of(7L, 8L));
	}
}
