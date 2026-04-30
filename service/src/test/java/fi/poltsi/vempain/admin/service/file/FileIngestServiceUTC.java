package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.api.request.file.FileIngestRequest;
import fi.poltsi.vempain.admin.api.response.file.FileIngestResponse;
import fi.poltsi.vempain.admin.configuration.StorageDirectoryConfiguration;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.entity.file.GalleryFile;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.exception.VempainIngestException;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.admin.service.SubjectService;
import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileIngestServiceUTC {

	@TempDir
	Path tempDir;

	@Mock
	private SiteFileRepository            siteFileRepository;
	@Mock
	private GalleryRepository             galleryRepository;
	@Mock
	private AclService                    aclService;
	@Mock
	private AccessService                 accessService;
	@Mock
	private GalleryFileService            galleryFileService;
	@Mock
	private StorageDirectoryConfiguration storageDirectoryConfiguration;
	@Mock
	private SubjectService                subjectService;
	@Mock
	private FileService                   fileService;
	@Mock
	private LocationService               locationService;

	@InjectMocks
	private FileIngestService fileIngestService;

	private static final byte[] FILE_CONTENT = "test-file-content".getBytes();
	private static final String SHA256_SUM    = DigestUtils.sha256Hex(FILE_CONTENT);

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(fileIngestService, "siteFileDirectory", tempDir.toString());
	}

	// ─── ingestInternal happy path ──────────────────────────────────────────────

	@Test
	void ingestInternal_newFile_createsAndReturnsResponse() throws Exception {
		when(storageDirectoryConfiguration.storageLocations())
				.thenReturn(Map.of("image", tempDir.toString()));
		when(siteFileRepository.findByFilePathAndFileName(any(), any()))
				.thenReturn(Optional.empty());
		when(accessService.getUserId()).thenReturn(1L);
		when(aclService.createNewAcl(anyLong(), isNull(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
				.thenReturn(10L);
		when(locationService.upsertAndGet(any())).thenReturn(null);

		var savedFile = SiteFile.builder().build();
		savedFile.setId(42L);
		when(fileService.saveSiteFile(any(SiteFile.class))).thenReturn(savedFile);

		var multipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", FILE_CONTENT);

		var request = FileIngestRequest.builder()
									   .fileName("test.jpg")
									   .mimeType("image/jpeg")
									   .sha256sum(SHA256_SUM)
									   .comment("")
									   .metadata("{}")
									   .tags(List.of())
									   .build();

		var response = fileIngestService.ingestInternal(request, multipartFile);

		assertNotNull(response);
		assertEquals(42L, response.getSiteFileId());
		assertNull(response.getGalleryId());
	}

	@Test
	void ingestInternal_existingFile_updatesAndReturnsResponse() throws VempainIngestException {
		when(storageDirectoryConfiguration.storageLocations())
				.thenReturn(Map.of("image", tempDir.toString()));

		var existingFile = SiteFile.builder().build();
		existingFile.setId(7L);
		when(siteFileRepository.findByFilePathAndFileName(any(), any()))
				.thenReturn(Optional.of(existingFile));
		when(accessService.getUserId()).thenReturn(1L);
		when(locationService.upsertAndGet(any())).thenReturn(null);

		var savedFile = SiteFile.builder().build();
		savedFile.setId(7L);
		when(fileService.saveSiteFile(any(SiteFile.class))).thenReturn(savedFile);
		when(galleryFileService.findGalleryFileByGalleryId(anyLong())).thenReturn(List.of());

		var multipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", FILE_CONTENT);

		var gallery = Gallery.builder().id(100L).shortname("Summer").build();
		when(galleryRepository.findById(100L)).thenReturn(Optional.of(gallery));

		var request = FileIngestRequest.builder()
									   .fileName("test.jpg")
									   .mimeType("image/jpeg")
									   .sha256sum(SHA256_SUM)
									   .comment("")
									   .metadata("{}")
									   .galleryId(100L)
									   .tags(List.of())
									   .build();

		var response = fileIngestService.ingestInternal(request, multipartFile);

		assertNotNull(response);
		assertEquals(7L, response.getSiteFileId());
		assertEquals(100L, response.getGalleryId());
	}

	@Test
	void ingestInternal_fileAlreadyInGallery_doesNotAddDuplicate() throws VempainIngestException {
		when(storageDirectoryConfiguration.storageLocations())
				.thenReturn(Map.of("image", tempDir.toString()));

		var existingFile = SiteFile.builder().build();
		existingFile.setId(7L);
		when(siteFileRepository.findByFilePathAndFileName(any(), any()))
				.thenReturn(Optional.of(existingFile));
		when(accessService.getUserId()).thenReturn(1L);
		when(locationService.upsertAndGet(any())).thenReturn(null);

		var savedFile = SiteFile.builder().build();
		savedFile.setId(7L);
		when(fileService.saveSiteFile(any(SiteFile.class))).thenReturn(savedFile);

		var gallery = Gallery.builder().id(100L).shortname("Summer").build();
		when(galleryRepository.findById(100L)).thenReturn(Optional.of(gallery));

		var alreadyLinked = GalleryFile.builder().galleryId(100L).siteFileId(7L).build();
		when(galleryFileService.findGalleryFileByGalleryId(100L)).thenReturn(List.of(alreadyLinked));

		var multipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", FILE_CONTENT);
		var request = FileIngestRequest.builder()
									   .fileName("test.jpg")
									   .mimeType("image/jpeg")
									   .sha256sum(SHA256_SUM)
									   .comment("")
									   .metadata("{}")
									   .galleryId(100L)
									   .tags(List.of())
									   .build();

		var response = fileIngestService.ingestInternal(request, multipartFile);

		assertNotNull(response);
		verify(galleryFileService).findGalleryFileByGalleryId(100L);
	}

	@Test
	void ingestInternal_sha256Mismatch_throwsVempainIngestException() {
		when(storageDirectoryConfiguration.storageLocations())
				.thenReturn(Map.of("image", tempDir.toString()));

		var multipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", FILE_CONTENT);
		var request = FileIngestRequest.builder()
									   .fileName("test.jpg")
									   .mimeType("image/jpeg")
									   .sha256sum("wrongchecksum")
									   .comment("")
									   .metadata("{}")
									   .tags(List.of())
									   .build();

		assertThrows(VempainIngestException.class, () -> fileIngestService.ingestInternal(request, multipartFile));
	}

	@Test
	void ingestInternal_unauthorizedUser_throwsVempainIngestException() {
		when(storageDirectoryConfiguration.storageLocations())
				.thenReturn(Map.of("image", tempDir.toString()));
		when(accessService.getUserId()).thenReturn(null);

		var multipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", FILE_CONTENT);
		var request = FileIngestRequest.builder()
									   .fileName("test.jpg")
									   .mimeType("image/jpeg")
									   .sha256sum(SHA256_SUM)
									   .comment("")
									   .metadata("{}")
									   .tags(List.of())
									   .build();

		assertThrows(VempainIngestException.class, () -> fileIngestService.ingestInternal(request, multipartFile));
	}

	// ─── validation ─────────────────────────────────────────────────────────────

	@Test
	void ingestInternal_nullRequest_throwsVempainIngestException() {
		var multipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", FILE_CONTENT);
		assertThrows(VempainIngestException.class,
					 () -> fileIngestService.ingestInternal(null, multipartFile));
	}

	@Test
	void ingestInternal_missingFileName_throwsVempainIngestException() {
		var multipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", FILE_CONTENT);
		var request = FileIngestRequest.builder()
									   .mimeType("image/jpeg")
									   .sha256sum(SHA256_SUM)
									   .comment("")
									   .metadata("{}")
									   .build();
		assertThrows(VempainIngestException.class,
					 () -> fileIngestService.ingestInternal(request, multipartFile));
	}

	@Test
	void ingestInternal_invalidMimetype_throwsVempainIngestException() {
		var multipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", FILE_CONTENT);
		var request = FileIngestRequest.builder()
									   .fileName("test.jpg")
									   .mimeType("not-a-mimetype")
									   .sha256sum(SHA256_SUM)
									   .comment("")
									   .metadata("{}")
									   .build();
		assertThrows(VempainIngestException.class,
					 () -> fileIngestService.ingestInternal(request, multipartFile));
	}

	@Test
	void ingestInternal_missingSha256_throwsVempainIngestException() {
		var multipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", FILE_CONTENT);
		var request = FileIngestRequest.builder()
									   .fileName("test.jpg")
									   .mimeType("image/jpeg")
									   .comment("")
									   .metadata("{}")
									   .build();
		assertThrows(VempainIngestException.class,
					 () -> fileIngestService.ingestInternal(request, multipartFile));
	}

	// ─── upsertGallery ──────────────────────────────────────────────────────────

	@Test
	void upsertGallery_galleryIdFound_returnsExistingGallery() {
		var gallery = Gallery.builder().id(1L).shortname("Old").description("Desc").build();
		when(galleryRepository.findById(1L)).thenReturn(Optional.of(gallery));

		var request = FileIngestRequest.builder()
									   .galleryId(1L)
									   .build();

		var result = fileIngestService.upsertGallery(request, 1L);
		assertNotNull(result);
		assertEquals(1L, result.getId());
	}

	@Test
	void upsertGallery_galleryIdFoundNameChanged_savesUpdatedGallery() {
		var gallery = Gallery.builder().id(1L).shortname("Old").description("Old desc").build();
		when(galleryRepository.findById(1L)).thenReturn(Optional.of(gallery));
		when(galleryRepository.save(any(Gallery.class))).thenAnswer(inv -> inv.getArgument(0));

		var request = FileIngestRequest.builder()
									   .galleryId(1L)
									   .galleryName("New Name")
									   .galleryDescription("New description")
									   .build();

		var result = fileIngestService.upsertGallery(request, 1L);
		assertNotNull(result);
		assertEquals("New Name", result.getShortname());
		verify(galleryRepository).save(any(Gallery.class));
	}

	@Test
	void upsertGallery_galleryNameProvided_createsNewGallery() throws Exception {
		when(galleryRepository.findByShortname("NewGallery")).thenReturn(Optional.empty());
		when(aclService.createNewAcl(anyLong(), isNull(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
				.thenReturn(20L);
		var saved = Gallery.builder().id(99L).shortname("NewGallery").build();
		when(galleryRepository.save(any(Gallery.class))).thenReturn(saved);

		var request = FileIngestRequest.builder()
									   .galleryName("NewGallery")
									   .galleryDescription("A new gallery")
									   .build();

		var result = fileIngestService.upsertGallery(request, 1L);
		assertNotNull(result);
		assertEquals(99L, result.getId());
	}

	@Test
	void upsertGallery_noGalleryInfo_returnsNull() {
		var request = FileIngestRequest.builder().build();
		var result = fileIngestService.upsertGallery(request, 1L);
		assertNull(result);
	}

	@Test
	void upsertGallery_aclCreationFails_returnsNull() throws Exception {
		when(galleryRepository.findByShortname("FailGallery")).thenReturn(Optional.empty());
		when(aclService.createNewAcl(anyLong(), isNull(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
				.thenThrow(new RuntimeException("ACL failure"));

		var request = FileIngestRequest.builder()
									   .galleryName("FailGallery")
									   .build();

		var result = fileIngestService.upsertGallery(request, 1L);
		assertNull(result);
	}

	// ─── ingest (public wrapper) ─────────────────────────────────────────────────

	@Test
	void ingest_onVempainIngestException_rethrowsCause() {
		when(storageDirectoryConfiguration.storageLocations())
				.thenReturn(Map.of("image", tempDir.toString()));
		when(accessService.getUserId()).thenReturn(0L);

		var multipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", FILE_CONTENT);
		var request = FileIngestRequest.builder()
									   .fileName("test.jpg")
									   .mimeType("image/jpeg")
									   .sha256sum(SHA256_SUM)
									   .comment("")
									   .metadata("{}")
									   .tags(List.of())
									   .build();

		assertThrows(Exception.class, () -> fileIngestService.ingest(request, multipartFile));
	}

	@Test
	void ingest_storageNotConfigured_throwsException() {
		when(storageDirectoryConfiguration.storageLocations())
				.thenReturn(Map.of());

		var multipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", FILE_CONTENT);
		var request = FileIngestRequest.builder()
									   .fileName("test.jpg")
									   .mimeType("image/jpeg")
									   .sha256sum(SHA256_SUM)
									   .comment("")
									   .metadata("{}")
									   .tags(List.of())
									   .build();

		assertThrows(Exception.class, () -> fileIngestService.ingest(request, multipartFile));
	}

	@Test
	void ingestInternal_unsupportedMimetypeFallsToOtherKey_ok() throws Exception {
		// "audio/mp3" is not in the map but "other" is - should use "other" fallback
		when(storageDirectoryConfiguration.storageLocations())
				.thenReturn(Map.of("other", tempDir.toString()));
		when(siteFileRepository.findByFilePathAndFileName(any(), any()))
				.thenReturn(Optional.empty());
		when(accessService.getUserId()).thenReturn(1L);
		when(aclService.createNewAcl(anyLong(), isNull(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
				.thenReturn(10L);
		when(locationService.upsertAndGet(any())).thenReturn(null);
		var savedFile = SiteFile.builder().build();
		savedFile.setId(42L);
		when(fileService.saveSiteFile(any(SiteFile.class))).thenReturn(savedFile);

		var multipartFile = new MockMultipartFile("file", "test.mp3", "audio/mp3", FILE_CONTENT);
		var request = FileIngestRequest.builder()
									   .fileName("test.mp3")
									   .mimeType("audio/mp3")
									   .sha256sum(SHA256_SUM)
									   .comment("")
									   .metadata("{}")
									   .tags(List.of())
									   .build();

		var response = fileIngestService.ingestInternal(request, multipartFile);
		assertNotNull(response);
		assertEquals(42L, response.getSiteFileId());
	}

	@Test
	void ingestInternal_unsupportedMimetypeNoOtherKey_throwsVempainIngestException() {
		// "audio/mp3" is not in the map and there is no "other" either
		when(storageDirectoryConfiguration.storageLocations())
				.thenReturn(Map.of("image", tempDir.toString()));

		var multipartFile = new MockMultipartFile("file", "test.mp3", "audio/mp3", FILE_CONTENT);
		var request = FileIngestRequest.builder()
									   .fileName("test.mp3")
									   .mimeType("audio/mp3")
									   .sha256sum(SHA256_SUM)
									   .comment("")
									   .metadata("{}")
									   .tags(List.of())
									   .build();

		assertThrows(VempainIngestException.class, () -> fileIngestService.ingestInternal(request, multipartFile));
	}

	@Test
	void ingestInternal_dotDotInFileName_throwsVempainIngestException() {
		when(storageDirectoryConfiguration.storageLocations())
				.thenReturn(Map.of("image", tempDir.toString()));

		var multipartFile = new MockMultipartFile("file", "evil..jpg", "image/jpeg", FILE_CONTENT);
		var request = FileIngestRequest.builder()
									   .fileName("evil..jpg")
									   .mimeType("image/jpeg")
									   .sha256sum(SHA256_SUM)
									   .comment("")
									   .metadata("{}")
									   .tags(List.of())
									   .build();

		assertThrows(VempainIngestException.class, () -> fileIngestService.ingestInternal(request, multipartFile));
	}

	@Test
	void ingestInternal_dotDotInRelativePath_throwsVempainIngestException() {
		when(storageDirectoryConfiguration.storageLocations())
				.thenReturn(Map.of("image", tempDir.toString()));

		var multipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", FILE_CONTENT);
		var request = FileIngestRequest.builder()
									   .fileName("test.jpg")
									   .filePath("../escape")
									   .mimeType("image/jpeg")
									   .sha256sum(SHA256_SUM)
									   .comment("")
									   .metadata("{}")
									   .tags(List.of())
									   .build();

		assertThrows(VempainIngestException.class, () -> fileIngestService.ingestInternal(request, multipartFile));
	}

}
