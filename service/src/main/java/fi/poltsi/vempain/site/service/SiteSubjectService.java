package fi.poltsi.vempain.site.service;

import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.site.entity.SiteSubject;
import fi.poltsi.vempain.site.repository.SiteSubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteSubjectService {
	private final SiteSubjectRepository siteSubjectRepository;

	public void saveAllFromAdminSubject(List<Subject> subjects) {
		for (Subject subject : subjects) {
			saveFromAdminSubject(subject);
		}
	}

	public SiteSubject saveFromAdminSubject(Subject subject) {
		var siteSubject = siteSubjectRepository.findBySubject(subject.getSubjectName());

		if (siteSubject != null) {
			siteSubject.setSubject(subject.getSubjectName());
			siteSubject.setSubjectDe(subject.getSubjectNameDe());
			siteSubject.setSubjectEn(subject.getSubjectNameEn());
			siteSubject.setSubjectFi(subject.getSubjectNameFi());
			siteSubject.setSubjectSe(subject.getSubjectNameSe());
			return siteSubjectRepository.save(siteSubject);
		} else {
			var newSiteSubject = SiteSubject.builder()
											.id(subject.getId())
											.subject(subject.getSubjectName())
											.subjectDe(subject.getSubjectNameDe())
											.subjectEn(subject.getSubjectNameEn())
											.subjectFi(subject.getSubjectNameFi())
											.subjectSe(subject.getSubjectNameSe())
											.build();
			return siteSubjectRepository.save(newSiteSubject);
		}
	}

	public void saveSiteFileSubject(long fileId, List<Subject> subjects) {
		for (Subject subject : subjects) {
			var siteSubject = siteSubjectRepository.findBySubject(subject.getSubjectName());
			var siteSubjectId = 0L;

			if (siteSubject == null) {
				var newSiteSubject = saveFromAdminSubject(subject);
				siteSubjectId = newSiteSubject.getId();
			} else {
				siteSubjectId = siteSubject.getId();
			}

			siteSubjectRepository.saveSiteFileSubject(fileId, siteSubjectId);
		}
	}
}
