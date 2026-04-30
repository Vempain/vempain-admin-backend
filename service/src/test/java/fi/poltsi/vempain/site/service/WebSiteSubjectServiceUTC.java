package fi.poltsi.vempain.site.service;

import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.site.entity.WebSiteSubject;
import fi.poltsi.vempain.site.repository.SiteSubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSiteSubjectServiceUTC {

	@Mock
	private SiteSubjectRepository siteSubjectRepository;

	@InjectMocks
	private WebSiteSubjectService webSiteSubjectService;

	private Subject buildSubject(long id, String name) {
		return Subject.builder()
					  .id(id)
					  .subjectName(name)
					  .subjectNameDe("de-" + name)
					  .subjectNameEn("en-" + name)
					  .subjectNameEs("es-" + name)
					  .subjectNameFi("fi-" + name)
					  .subjectNameSe("se-" + name)
					  .build();
	}

	private WebSiteSubject buildSiteSubject(long id, String name) {
		return WebSiteSubject.builder()
							 .id(id)
							 .subjectName(name)
							 .subjectNameDe("de-" + name)
							 .subjectNameEn("en-" + name)
							 .subjectNameEs("es-" + name)
							 .subjectNameFi("fi-" + name)
							 .subjectNameSe("se-" + name)
							 .build();
	}

	@Test
	void saveFromAdminSubjectNewOk() {
		var subject = buildSubject(1L, "nature");
		var saved = buildSiteSubject(10L, "nature");

		when(siteSubjectRepository.findBySubjectName("nature")).thenReturn(null);
		when(siteSubjectRepository.save(any(WebSiteSubject.class))).thenReturn(saved);

		var result = webSiteSubjectService.saveFromAdminSubject(subject);

		assertNotNull(result);
		assertEquals("nature", result.getSubjectName());
		verify(siteSubjectRepository).save(any(WebSiteSubject.class));
	}

	@Test
	void saveFromAdminSubjectExistingUpdatesOk() {
		var subject = buildSubject(1L, "nature");
		var existing = buildSiteSubject(10L, "nature");
		existing.setSubjectNameEn("old-en");

		when(siteSubjectRepository.findBySubjectName("nature")).thenReturn(existing);
		when(siteSubjectRepository.save(any(WebSiteSubject.class))).thenReturn(existing);

		var result = webSiteSubjectService.saveFromAdminSubject(subject);

		assertNotNull(result);
		assertEquals("en-nature", result.getSubjectNameEn());
		verify(siteSubjectRepository).save(existing);
	}

	@Test
	void saveAllFromAdminSubjectOk() {
		var subjects = List.of(buildSubject(1L, "nature"), buildSubject(2L, "travel"));
		var savedNature = buildSiteSubject(10L, "nature");
		var savedTravel = buildSiteSubject(11L, "travel");

		when(siteSubjectRepository.findBySubjectName("nature")).thenReturn(null);
		when(siteSubjectRepository.findBySubjectName("travel")).thenReturn(null);
		when(siteSubjectRepository.save(any(WebSiteSubject.class)))
				.thenReturn(savedNature)
				.thenReturn(savedTravel);

		var result = webSiteSubjectService.saveAllFromAdminSubject(subjects);

		assertNotNull(result);
		assertEquals(2, result.size());
	}

	@Test
	void saveAllFromAdminSubjectEmptyOk() {
		var result = webSiteSubjectService.saveAllFromAdminSubject(List.of());

		assertNotNull(result);
		assertEquals(0, result.size());
	}

	@Test
	void saveSiteFileSubjectOk() {
		var subject1 = buildSiteSubject(10L, "nature");
		var subject2 = buildSiteSubject(11L, "travel");

		doNothing().when(siteSubjectRepository).saveSiteFileSubject(anyLong(), anyLong());

		webSiteSubjectService.saveSiteFileSubject(100L, List.of(subject1, subject2));

		verify(siteSubjectRepository, times(2)).saveSiteFileSubject(anyLong(), anyLong());
	}

	@Test
	void saveSiteFileSubjectEmptyListOk() {
		webSiteSubjectService.saveSiteFileSubject(100L, List.of());

		verify(siteSubjectRepository, times(0)).saveSiteFileSubject(anyLong(), anyLong());
	}
}
