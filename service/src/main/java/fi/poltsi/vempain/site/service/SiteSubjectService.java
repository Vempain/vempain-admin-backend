package fi.poltsi.vempain.site.service;

import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.site.entity.WebSiteSubject;
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

	public List<WebSiteSubject> saveAllFromAdminSubject(List<Subject> subjects) {
		var siteSubjects = new ArrayList<WebSiteSubject>();

		for (Subject subject : subjects) {
			var siteSubject = saveFromAdminSubject(subject);
			log.debug("Saving subject {} with ID {} as site subject ID {}", subject.getSubjectName(), subject.getId(), siteSubject.getId());
			siteSubjects.add(siteSubject);
		}

		return siteSubjects;
	}

	public WebSiteSubject saveFromAdminSubject(Subject subject) {
		var siteSubject = siteSubjectRepository.findBySubjectName(subject.getSubjectName());
		if (siteSubject != null) {
			log.debug("Found existing site subject for subject {}: {}", subject.getSubjectName(), siteSubject.getId());
			// We update the existing site subject translations
			siteSubject.setSubjectNameDe(subject.getSubjectNameDe());
			siteSubject.setSubjectNameEn(subject.getSubjectNameEn());
			siteSubject.setSubjectNameEs(subject.getSubjectNameEs());
			siteSubject.setSubjectNameFi(subject.getSubjectNameFi());
			siteSubject.setSubjectNameSe(subject.getSubjectNameSe());
			return siteSubjectRepository.save(siteSubject);
		} else {
			var newSiteSubject = WebSiteSubject.builder()
											   .subjectName(subject.getSubjectName())
											   .subjectNameDe(subject.getSubjectNameDe())
											   .subjectNameEn(subject.getSubjectNameEn())
											   .subjectNameEs(subject.getSubjectNameEs())
											   .subjectNameFi(subject.getSubjectNameFi())
											   .subjectNameSe(subject.getSubjectNameSe())
											   .build();
			var newSiteSubjectSaved = siteSubjectRepository.save(newSiteSubject);
			log.debug("Created new site subject for subject {} with ID {}: {}", subject.getSubjectName(), subject.getId(), newSiteSubjectSaved.getId());
			return newSiteSubjectSaved;
		}
	}

	@Transactional
	public void saveSiteFileSubject(long fileId, List<WebSiteSubject> webSiteSubjects) {
		for (var siteSubject : webSiteSubjects) {
			siteSubjectRepository.saveSiteFileSubject(fileId, siteSubject.getId());
		}
	}
}
