package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.admin.repository.file.SubjectRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class SubjectService {
	private final SubjectRepository            subjectRepository;
	private final EntityManager				entityManager;

	public Optional<Subject> getSubjectById(long subjectId) {
		return subjectRepository.findById(subjectId);
	}

	public Map<Long, List<Subject>> getCommonFileSubjectListMap(long[] fileCommonIds) {
		var commonFileIdSubjectMap = new HashMap<Long, List<Subject>>();

		for (long fileCommonId : fileCommonIds) {
			var subjects = subjectRepository.getSubjectsByFileId(fileCommonId);
			commonFileIdSubjectMap.put(fileCommonId, subjects);
		}

		return commonFileIdSubjectMap;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void addSubjectToFile(Long subjectId, Long fileCommonId) {
		entityManager.createNativeQuery("INSERT INTO file_subject (subject_id, file_common_id) VALUES (:subjectId, :fileCommonId)")
					 .setParameter("subjectId", subjectId)
					 .setParameter("fileCommonId", fileCommonId)
					 .executeUpdate();
	}

	public List<Subject> getSubjectsByFileId(long fileCommonId) {
		return subjectRepository.getSubjectsByFileId(fileCommonId);
	}
}
