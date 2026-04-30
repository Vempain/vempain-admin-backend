package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteFileServiceUTC {

	@Mock
	private SiteFileRepository siteFileRepository;

	@InjectMocks
	private SiteFileService siteFileService;

	// ---- save ----

	@Test
	void saveOk() {
		SiteFile siteFile = buildSiteFile(1L);
		when(siteFileRepository.save(any(SiteFile.class))).thenReturn(siteFile);

		SiteFile result = siteFileService.save(siteFile);

		assertNotNull(result);
		assertEquals(1L, result.getId());
		verify(siteFileRepository).save(siteFile);
	}

	@Test
	void saveReturnsPersistedEntityOk() {
		SiteFile input = buildSiteFile(0L);
		SiteFile saved = buildSiteFile(42L);
		when(siteFileRepository.save(input)).thenReturn(saved);

		SiteFile result = siteFileService.save(input);

		assertNotNull(result);
		assertEquals(42L, result.getId());
	}

	@Test
	void saveUpdatesExistingEntityOk() {
		SiteFile existing = buildSiteFile(5L);
		existing.setComment("Updated comment");
		when(siteFileRepository.save(existing)).thenReturn(existing);

		SiteFile result = siteFileService.save(existing);

		assertNotNull(result);
		assertEquals("Updated comment", result.getComment());
	}

	private SiteFile buildSiteFile(long id) {
		return SiteFile.builder()
					   .id(id)
					   .fileName("test-file.jpg")
					   .filePath("/uploads/test/")
					   .mimeType("image/jpeg")
					   .size(1024L)
					   .fileType(FileTypeEnum.IMAGE)
					   .comment("Test comment")
					   .metadata("{}")
					   .sha256sum("abc123")
					   .creator(1L)
					   .created(Instant.now())
					   .build();
	}
}
