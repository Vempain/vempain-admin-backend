package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.service.SubjectService;
import fi.poltsi.vempain.admin.service.file.SiteFileService;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class SiteFileRepositoryITC extends AbstractITCTest {
	@Autowired
	private SubjectService    subjectService;
	@Autowired
	private SiteFileService siteFileService;

	@Test
	void getSubjectsByFileId() {
		var userId = testITCTools.generateUser();

		var subject = Subject.builder()
							 .subjectName("subjectName")
							 .subjectNameDe("de")
							 .subjectNameEn("en")
							 .subjectNameFi("fi")
							 .subjectNameSe("se")
							 .subjectNameEs("es")
							 .build();
		subject = subjectService.save(subject);
		log.info("Created subject with id {}", subject.getId());
		var nextFileId = siteFileRepository.findMaxFileId();
		nextFileId++;
		var siteFile = SiteFile.builder()
							   .fileId(nextFileId)
							   .filePath("filePath")
							   .fileName("fileName")
							   .mimeType("image/jpeg")
							   .sha256sum("Test-SHA256-Sum")
							   .fileType(FileTypeEnum.IMAGE)
							   .creator(userId)
							   .created(Instant.now())
							   .modifier(userId)
							   .modified(Instant.now())
							   .build();

		siteFile = siteFileService.save(siteFile);
		log.info("Created site file with id {}", siteFile.getId());
		subjectService.addSubjectToFile(siteFile.getId(), subject.getId());

		var subjects = subjectService.getSubjectsByFileId(siteFile.getId());

		assertEquals(1, subjects.size());
	}

	@Test
	void findByFileTypeReturnsCorrectResults() {
		var userId = testITCTools.generateUser();

		var aclId1 = testITCTools.generateAcl(userId, null, true, true, true, true);
		var nextFileId = siteFileRepository.findMaxFileId();
		nextFileId++;
		var siteFile1 = SiteFile.builder()
								.fileId(nextFileId)
								.aclId(aclId1)
								.filePath("path1")
								.fileName("file1")
								.mimeType("image/jpeg")
								.sha256sum("SHA256-1")
								.fileType(FileTypeEnum.IMAGE)
								.creator(userId)
								.created(Instant.now())
								.modifier(userId)
								.modified(Instant.now())
								.build();
		siteFileRepository.save(siteFile1);

		var aclId2 = testITCTools.generateAcl(userId, null, true, true, true, true);
		nextFileId++;
		var siteFile2 = SiteFile.builder()
								.fileId(nextFileId)
								.aclId(aclId2)
								.filePath("path2")
								.fileName("file2")
								.mimeType("application/pdf")
								.sha256sum("SHA256-2")
								.fileType(FileTypeEnum.DOCUMENT)
								.creator(userId)
								.created(Instant.now())
								.modifier(userId)
								.modified(Instant.now())
								.build();
		siteFileRepository.save(siteFile2);

		var pageable = PageRequest.of(0, 10);

		var result = siteFileRepository.findByFileType(FileTypeEnum.IMAGE, pageable);

		assertEquals(1, result.getTotalElements());
		assertEquals("file1", result.getContent()
									.getFirst()
									.getFileName());
	}

	@Test
	void findByFileTypeReturnsEmptyWhenNoMatch() {
		var pageable = PageRequest.of(0, 10);

		var result = siteFileRepository.findByFileType(FileTypeEnum.VIDEO, pageable);

		assertEquals(0, result.getTotalElements());
	}
}
