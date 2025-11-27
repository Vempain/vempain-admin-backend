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
		// Ensure all pending inserts (including SiteFile saved by caller) are flushed
		// so the FK from file_subject(site_file_id) can reference an existing row.
		entityManager.flush();

		removeAllSubjectsFromFile(siteFileId);

		for (TagRequest tagRequest : tagRequests) {
			var tagName = tagRequest.getTagName();
			// Check if the subject already exists in the subject table
			var subject = subjectRepository.findSubjectBySubjectName(tagName)
										   .orElse(null);

			if (subject == null) {
				var newSubject = Subject.builder()
										.subjectName(tagName)
										.subjectNameDe(tagRequest.getTagNameDe())
										.subjectNameEn(tagRequest.getTagNameEn())
										.subjectNameFi(tagRequest.getTagNameFi())
										.subjectNameSe(tagRequest.getTagNameSv())
										.subjectNameEs(tagRequest.getTagNameEs())
										.build();
				try {
					subject = subjectRepository.save(newSubject);
				} catch (Exception e) {
					log.warn("Error saving new subject {}: {}", tagName, e.getMessage());
					subject = subjectRepository.findSubjectBySubjectName(tagName)
											   .orElse(null);

					if (subject == null) {
						log.error("Failed to retrieve subject {} after save error", tagName);
						continue;
					}
					subject = updateExistingSubject(tagRequest, subject);
				}
			} else {
				subject = updateExistingSubject(tagRequest, subject);
			}

			// Link subject to file (FK order: site_file_id, subject_id)
			addSubjectToFile(siteFileId, subject.getId());
		}
	}

	private Subject updateExistingSubject(TagRequest tagRequest, Subject subject) {
		// Update existing subject names in other languages if they are missing or have changed
		boolean updated = false;

		if (tagRequest.getTagNameDe() != null
			&& !tagRequest.getTagNameDe()
						  .equals(subject.getSubjectNameDe())) {
			subject.setSubjectNameDe(tagRequest.getTagNameDe());
			updated = true;
		}
		if (tagRequest.getTagNameEn() != null
			&& !tagRequest.getTagNameEn()
						  .equals(subject.getSubjectNameEn())) {
			subject.setSubjectNameEn(tagRequest.getTagNameEn());
			updated = true;
		}
		if (tagRequest.getTagNameFi() != null
			&& !tagRequest.getTagNameFi()
						  .equals(subject.getSubjectNameFi())) {
			subject.setSubjectNameFi(tagRequest.getTagNameFi());
			updated = true;
		}
		if (tagRequest.getTagNameSv() != null
			&& !tagRequest.getTagNameSv()
						  .equals(subject.getSubjectNameSe())) {
			subject.setSubjectNameSe(tagRequest.getTagNameSv());
			updated = true;
		}
		if (tagRequest.getTagNameEs() != null
			&& !tagRequest.getTagNameEs()
						  .equals(subject.getSubjectNameEs())) {
			subject.setSubjectNameEs(tagRequest.getTagNameEs());
			updated = true;
		}
		if (updated) {
			subject = subjectRepository.save(subject);
		}
		return subject;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void addSubjectToFile(Long siteFileId, Long subjectId) {
		entityManager.createNativeQuery(
							 "INSERT INTO file_subject (site_file_id, subject_id) VALUES (:siteFileId, :subjectId)")
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
