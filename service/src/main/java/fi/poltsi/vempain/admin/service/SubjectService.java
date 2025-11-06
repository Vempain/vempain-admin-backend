package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.admin.repository.file.SubjectRepository;
import fi.poltsi.vempain.file.api.request.TagRequest;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SubjectService {
	private final SubjectRepository subjectRepository;
	private final EntityManager     entityManager;

	@Transactional(propagation = Propagation.REQUIRED)
	public void saveTagsAsSubjects(List<TagRequest> tagRequests, long siteFileId) {
		removeAllSubjectsFromFile(siteFileId);

		for (TagRequest tagRequest : tagRequests) {
			// Check if the subject already exists in the subject table
			var existingSubject = subjectRepository.findSubjectBySubjectName(tagRequest.getTagName());
			var subject = existingSubject.orElse(null);

			if (existingSubject.isEmpty()) {
				var newSubject = Subject.builder()
										.subjectName(tagRequest.getTagName())
										.subjectNameDe(tagRequest.getTagNameDe())
										.subjectNameEn(tagRequest.getTagNameEn())
										.subjectNameFi(tagRequest.getTagNameFi())
										.subjectNameSe(tagRequest.getTagNameSv())
										.subjectNameEs(tagRequest.getTagNameEs())
										.build();
				subject = subjectRepository.save(newSubject);
			}

			addSubjectToFile(siteFileId, subject.getId());
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void addSubjectToFile(Long siteFileId, Long subjectId) {
		entityManager.createNativeQuery("INSERT INTO file_subject (site_file_id, subject_id) VALUES (:siteFileId, :subjectId)")
					 .setParameter("siteFileId", siteFileId)
					 .setParameter("subjectId", subjectId)
					 .executeUpdate();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void removeAllSubjectsFromFile(long siteFileId) {
		entityManager.createNativeQuery("DELETE FROM file_subject WHERE site_file_id = :siteFileId")
					 .setParameter("siteFileId", siteFileId)
					 .executeUpdate();
	}

	public List<Subject> getSubjectsByFileId(long fileCommonId) {
		return subjectRepository.getSubjectsByFileId(fileCommonId);
	}
}
