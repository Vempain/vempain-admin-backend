package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.FileClassEnum;
import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.service.SubjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SiteFileRepositoryITC extends AbstractITCTest {
	@Autowired
	private SubjectRepository subjectRepository;
	@Autowired
	private SubjectService    subjectService;

	@Test
	void getSubjectsByFileId() {
		var userId = testITCTools.generateUser();

		var subject = Subject.builder()
							 .subjectName("subjectName")
							 .subjectNameDe("de")
							 .subjectNameEn("en")
							 .subjectNameFi("fi")
							 .subjectNameSe("se")
							 .build();
		subject = subjectRepository.save(subject);

		var siteFile = SiteFile.builder()
							   .filePath("filePath")
							   .fileName("fileName")
							   .mimeType("image/jpeg")
							   .sha256sum("Test-SHA256-Sum")
							   .fileClass(FileClassEnum.IMAGE)
							   .creator(userId)
							   .created(Instant.now())
							   .modifier(userId)
							   .modified(Instant.now())
							   .build();

		siteFile = siteFileRepository.save(siteFile);

		subjectService.addSubjectToFile(subject.getId(), siteFile.getId());

		var subjects = subjectService.getSubjectsByFileId(siteFile.getId());

		assertEquals(1, subjects.size());
	}
}
