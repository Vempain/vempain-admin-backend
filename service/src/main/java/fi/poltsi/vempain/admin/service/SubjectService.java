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

	// Java
	private Long upsertSubjectReturnId(TagRequest tagRequest) {
		var sql = """
				INSERT INTO subjects (subject, subject_de, subject_en, subject_es, subject_fi, subject_se)
				VALUES (:subject, :de, :en, :es, :fi, :se)
				ON CONFLICT (subject) DO UPDATE
				  SET subject = EXCLUDED.subject
				RETURNING id
				""";

		var resList = entityManager.createNativeQuery(sql)
								   .setParameter("subject", tagRequest.getTagName())
								   .setParameter("de", tagRequest.getTagNameDe())
								   .setParameter("en", tagRequest.getTagNameEn())
								   .setParameter("es", tagRequest.getTagNameEs())
								   .setParameter("fi", tagRequest.getTagNameFi())
								   .setParameter("se", tagRequest.getTagNameSv())
								   .getResultList();

		if (resList.isEmpty()) {
			throw new IllegalStateException("Upsert returned no id for subject: " + tagRequest.getTagName());
		}

		Object res = resList.getFirst();

		if (res instanceof Number) {
			return ((Number) res).longValue();
		}

		throw new IllegalStateException("Unexpected id type: " + (res == null ? "null" : res.getClass()));
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void saveTagsAsSubjects(List<TagRequest> tagRequests, long siteFileId) {
		entityManager.flush();
		log.debug("Removing all subjects for file id {}", siteFileId);
		removeAllSubjectsFromFile(siteFileId);

		for (TagRequest tagRequest : tagRequests) {
			var tagName = tagRequest.getTagName();
			log.debug("Adding/updating subject for tag: {}", tagName);
			var subject = subjectRepository.findSubjectBySubjectName(tagName)
										   .orElse(null);

			if (subject == null) {
				log.debug("Subject {} not found, creating new", tagName);
				Long subjectId = upsertSubjectReturnId(tagRequest);
				subject = subjectRepository.findById(subjectId)
										   .orElse(null);
				if (subject == null) {
					log.error("Failed to load subject {} after upsert, id={}", tagName, subjectId);
					continue;
				}
				subject = updateExistingSubject(tagRequest, subject);
			} else {
				log.debug("Subject {} found with id {}, updating if necessary", tagName, subject.getId());
				subject = updateExistingSubject(tagRequest, subject);
			}

			log.debug("Linking subject id {} to file id {}", subject.getId(), siteFileId);
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

	@Transactional
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

	@Transactional
	public Subject save(Subject subject) {
		return subjectRepository.save(subject);
	}
}
