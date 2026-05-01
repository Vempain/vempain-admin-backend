package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.admin.repository.file.SubjectRepository;
import fi.poltsi.vempain.file.api.request.TagRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
class SubjectServiceUTC {

	@Mock
	private SubjectRepository subjectRepository;
	@Mock
	private EntityManager     entityManager;
	@Mock
	private Query             query;

	@InjectMocks
	private SubjectService subjectService;

	@BeforeEach
	void setUp() {
		when(entityManager.createNativeQuery(anyString())).thenReturn(query);
		when(query.setParameter(anyString(), any())).thenReturn(query);
	}

	// ---- saveTagsAsSubjects ----

	@Test
	void saveTagsAsSubjectsNewSubjectCreatedOk() {
		TagRequest tagRequest = mock(TagRequest.class);
		when(tagRequest.getTagName()).thenReturn("nature");
		when(tagRequest.getTagNameDe()).thenReturn("Natur");
		when(tagRequest.getTagNameEn()).thenReturn("nature");
		when(tagRequest.getTagNameFi()).thenReturn("luonto");
		when(tagRequest.getTagNameSv()).thenReturn("natur");
		when(tagRequest.getTagNameEs()).thenReturn("naturaleza");

		Subject subject = buildSubject(1L, "nature");
		when(subjectRepository.findSubjectBySubjectName("nature")).thenReturn(Optional.empty());
		// upsertSubjectReturnId returns 1L from native query
		when(query.getResultList()).thenReturn(List.of(1L)).thenReturn(List.of());
		when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
		when(subjectRepository.save(any(Subject.class))).thenReturn(subject);

		// executeUpdate for removeAllSubjectsFromFile and addSubjectToFile
		when(query.executeUpdate()).thenReturn(1);

		subjectService.saveTagsAsSubjects(List.of(tagRequest), 10L);

		verify(subjectRepository).findSubjectBySubjectName("nature");
	}

	@Test
	void saveTagsAsSubjectsExistingSubjectUpdatedOk() {
		TagRequest tagRequest = mock(TagRequest.class);
		when(tagRequest.getTagName()).thenReturn("mountain");
		when(tagRequest.getTagNameDe()).thenReturn("Berg");
		when(tagRequest.getTagNameEn()).thenReturn("mountain");
		when(tagRequest.getTagNameFi()).thenReturn("vuori");
		when(tagRequest.getTagNameSv()).thenReturn("berg");
		when(tagRequest.getTagNameEs()).thenReturn("montaña");

		Subject existing = buildSubject(2L, "mountain");
		when(subjectRepository.findSubjectBySubjectName("mountain")).thenReturn(Optional.of(existing));
		when(subjectRepository.save(any(Subject.class))).thenReturn(existing);
		when(query.executeUpdate()).thenReturn(1);

		subjectService.saveTagsAsSubjects(List.of(tagRequest), 5L);

		verify(subjectRepository).findSubjectBySubjectName("mountain");
	}

	@Test
	void saveTagsAsSubjectsEmptyListOk() {
		when(query.executeUpdate()).thenReturn(0);

		subjectService.saveTagsAsSubjects(List.of(), 10L);

		// Just verify flush was called (no exception)
		verify(entityManager).flush();
	}

	// ---- addSubjectToFile ----

	@Test
	void addSubjectToFileOk() {
		when(query.executeUpdate()).thenReturn(1);

		subjectService.addSubjectToFile(10L, 20L);

		verify(query).executeUpdate();
	}

	// ---- removeAllSubjectsFromFile ----

	@Test
	void removeAllSubjectsFromFileOk() {
		when(query.executeUpdate()).thenReturn(3);

		subjectService.removeAllSubjectsFromFile(10L);

		verify(query).executeUpdate();
	}

	// ---- getSubjectsByFileId ----

	@Test
	void getSubjectsByFileIdOk() {
		Subject s1 = buildSubject(1L, "ocean");
		Subject s2 = buildSubject(2L, "sea");
		when(subjectRepository.getSubjectsByFileId(5L)).thenReturn(List.of(s1, s2));

		List<Subject> result = subjectService.getSubjectsByFileId(5L);

		assertNotNull(result);
		assertEquals(2, result.size());
	}

	@Test
	void getSubjectsByFileIdEmptyOk() {
		when(subjectRepository.getSubjectsByFileId(99L)).thenReturn(List.of());

		List<Subject> result = subjectService.getSubjectsByFileId(99L);

		assertNotNull(result);
		assertEquals(0, result.size());
	}

	// ---- save ----

	@Test
	void saveOk() {
		Subject subject = buildSubject(1L, "forest");
		when(subjectRepository.save(subject)).thenReturn(subject);

		Subject result = subjectService.save(subject);

		assertNotNull(result);
		assertEquals("forest", result.getSubjectName());
	}

	// ---- upsertSubjectReturnId - edge case: returns no id ----

	@Test
	void saveTagsAsSubjectsUpsertReturnsNoIdThrowsFail() {
		TagRequest tagRequest = mock(TagRequest.class);
		when(tagRequest.getTagName()).thenReturn("river");

		when(subjectRepository.findSubjectBySubjectName("river")).thenReturn(Optional.empty());
		// upsertSubjectReturnId returns empty list (triggers IllegalStateException)
		when(query.getResultList()).thenReturn(List.of());
		when(query.executeUpdate()).thenReturn(1);

		// upsertSubjectReturnId throws IllegalStateException when result list is empty
		assertThrows(IllegalStateException.class,
				() -> subjectService.saveTagsAsSubjects(List.of(tagRequest), 10L));
	}

	private Subject buildSubject(long id, String name) {
		return Subject.builder()
					  .id(id)
					  .subjectName(name)
					  .subjectNameDe(name + "-de")
					  .subjectNameEn(name + "-en")
					  .subjectNameFi(name + "-fi")
					  .subjectNameSe(name + "-se")
					  .build();
	}
}
