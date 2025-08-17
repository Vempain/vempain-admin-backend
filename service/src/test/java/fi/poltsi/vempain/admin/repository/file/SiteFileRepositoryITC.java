package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.FileClassEnum;
import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.service.SubjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

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

	@Test
	 void findByFileClassReturnsCorrectResults() {
	  var userId = testITCTools.generateUser();

	  var siteFile1 = SiteFile.builder()
	          .filePath("path1")
	          .fileName("file1")
	          .mimeType("image/jpeg")
	          .sha256sum("SHA256-1")
	          .fileClass(FileClassEnum.IMAGE)
	          .creator(userId)
	          .created(Instant.now())
	          .modifier(userId)
	          .modified(Instant.now())
	          .build();
	  siteFileRepository.save(siteFile1);

	  var siteFile2 = SiteFile.builder()
	          .filePath("path2")
	          .fileName("file2")
	          .mimeType("application/pdf")
	          .sha256sum("SHA256-2")
	          .fileClass(FileClassEnum.DOCUMENT)
	          .creator(userId)
	          .created(Instant.now())
	          .modifier(userId)
	          .modified(Instant.now())
	          .build();
	  siteFileRepository.save(siteFile2);

	  var pageRequest = PageRequest.of(0, 10);
	  var pageable = PageRequest.of(0, 10);

	  var result = siteFileRepository.findByFileClass(FileClassEnum.IMAGE, pageRequest, pageable);

	  assertEquals(1, result.getTotalElements());
	  assertEquals("file1", result.getContent().getFirst().getFileName());
	 }

	 @Test
	 void findByFileClassReturnsEmptyWhenNoMatch() {
	  var pageRequest = PageRequest.of(0, 10);
	  var pageable = PageRequest.of(0, 10);

	  var result = siteFileRepository.findByFileClass(FileClassEnum.VIDEO, pageRequest, pageable);

	  assertEquals(0, result.getTotalElements());
	 }
}
