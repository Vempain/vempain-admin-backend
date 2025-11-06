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
		var siteSubject = siteSubjectRepository.findBySubjectName(subject.getSubjectName());

		if (siteSubject != null) {
			// We update the existing site subject translations
			siteSubject.setSubjectNameDe(subject.getSubjectNameDe());
			siteSubject.setSubjectNameEn(subject.getSubjectNameEn());
			siteSubject.setSubjectNameEs(subject.getSubjectNameEs());
			siteSubject.setSubjectNameFi(subject.getSubjectNameFi());
			siteSubject.setSubjectNameSe(subject.getSubjectNameSe());
			return siteSubjectRepository.save(siteSubject);
		} else {
			var newSiteSubject = SiteSubject.builder()
											.subjectName(subject.getSubjectName())
											.subjectNameDe(subject.getSubjectNameDe())
											.subjectNameEn(subject.getSubjectNameEn())
											.subjectNameEs(subject.getSubjectNameEs())
											.subjectNameFi(subject.getSubjectNameFi())
											.subjectNameSe(subject.getSubjectNameSe())
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
