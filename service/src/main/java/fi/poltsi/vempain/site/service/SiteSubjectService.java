package fi.poltsi.vempain.site.service;

import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.site.entity.SiteSubject;
import fi.poltsi.vempain.site.repository.SiteSubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteSubjectService {
	private final SiteSubjectRepository siteSubjectRepository;

	public List<SiteSubject> saveAllFromAdminSubject(List<Subject> subjects) {
		var siteSubjects = new ArrayList<SiteSubject>();

		for (Subject subject : subjects) {
			siteSubjects.add(saveFromAdminSubject(subject));
		}

		return siteSubjects;
	}

	public SiteSubject saveFromAdminSubject(Subject subject) {
		var siteSubject = siteSubjectRepository.findBySubject(subject.getSubjectName());

		if (siteSubject != null) {
			siteSubject.setSubjectId(subject.getId());
			// We update the existing site subject translations
			siteSubject.setSubjectDe(subject.getSubjectNameDe());
			siteSubject.setSubjectEn(subject.getSubjectNameEn());
			siteSubject.setSubjectFi(subject.getSubjectNameFi());
			siteSubject.setSubjectSe(subject.getSubjectNameSe());
			return siteSubjectRepository.save(siteSubject);
		} else {
			var newSiteSubject = SiteSubject.builder()
											.subjectId(subject.getId())
											.subject(subject.getSubjectName())
											.subjectDe(subject.getSubjectNameDe())
											.subjectEn(subject.getSubjectNameEn())
											.subjectFi(subject.getSubjectNameFi())
											.subjectSe(subject.getSubjectNameSe())
											.build();
			return siteSubjectRepository.save(newSiteSubject);
		}
	}

	@Transactional
	public void saveSiteFileSubject(long fileId, List<SiteSubject> siteSubjects) {
		for (var siteSubject : siteSubjects) {
			siteSubjectRepository.saveSiteFileSubject(fileId, siteSubject.getId());
		}
	}
}
