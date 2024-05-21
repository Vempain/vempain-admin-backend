package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.admin.entity.file.FileCommon;
import fi.poltsi.vempain.admin.service.SubjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileCommonPageableRepositoryITC extends AbstractITCTest {
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

		var fileCommon = FileCommon.builder()
								   .acls(null)
								   .fileClassId(1L)
								   .comment("comment")
								   .created(Instant.now())
								   .creator(userId)
								   .metadata(null)
								   .locked(false)
								   .mimetype("mimetype")
								   .convertedFile("sourceFile")
								   .convertedFilesize(1L)
								   .convertedSha1sum("sourceSha1sum")
								   .build();

		fileCommon = fileCommonPageableRepository.save(fileCommon);

		subjectService.addSubjectToFile(subject.getId(), fileCommon.getId());

		var subjects = subjectService.getSubjectsByFileId(fileCommon.getId());

		assertEquals(1, subjects.size());
	}
}
