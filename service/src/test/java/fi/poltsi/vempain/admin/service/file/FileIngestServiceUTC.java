package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.api.request.file.FileIngestAclRequest;
import fi.poltsi.vempain.admin.api.request.file.FileIngestRequest;
import fi.poltsi.vempain.admin.configuration.StorageDirectoryConfiguration;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.entity.file.GalleryFile;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.exception.VempainIngestException;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.admin.service.SubjectService;
import fi.poltsi.vempain.auth.api.AccountStatus;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.auth.service.UserService;
import fi.poltsi.vempain.common.api.FileTypeEnum;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
	@Mock
	private UserService userService;

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

		when(galleryRepository.save(any(Gallery.class)))
				.thenReturn(gallery);

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

		when(galleryRepository.save(any(Gallery.class)))
				.thenReturn(gallery);

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
		when(galleryRepository.findById(1L))
				.thenReturn(Optional.of(gallery));
		when(galleryRepository.save(any(Gallery.class)))
				.thenReturn(gallery);
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


	// ─── deleteIngestedSiteFile ─────────────────────────────────────────────────

	@Test
	void deleteIngestedSiteFile_removesStoredFileLinksSubjectsThumbAndAcl() throws Exception {
		var imageDir = Files.createDirectories(tempDir.resolve("images/trip"));
		var stored = Files.write(imageDir.resolve("photo.jpg"), FILE_CONTENT);
		when(storageDirectoryConfiguration.storageLocations()).thenReturn(Map.of("image", tempDir.resolve("images")
																								 .toString()));
		var siteFile = SiteFile.builder()
							   .fileName("photo.jpg")
							   .filePath("trip")
							   .fileType(FileTypeEnum.IMAGE)
							   .build();
		siteFile.setId(42L);
		siteFile.setAclId(10L);
		var thumb = new fi.poltsi.vempain.admin.entity.file.FileThumb();
		when(fileService.findSiteFileById(42L)).thenReturn(Optional.of(siteFile));
		when(fileService.findAllFileThumbsBySiteFileList(List.of(siteFile))).thenReturn(List.of(thumb));

		fileIngestService.deleteIngestedSiteFile(42L);

		assertFalse(Files.exists(stored));
		verify(fileService).deleteFileThumb(thumb);
		verify(galleryFileService).deleteGalleryFilesBySiteFileId(42L);
		verify(subjectService).removeAllSubjectsFromFile(42L);
		verify(siteFileRepository).delete(siteFile);
		verify(aclService).deleteByAclId(10L);
	}

	@Test
	void deleteIngestedSiteFile_toleratesMissingStoredFileAndMissingAcl() throws Exception {
		when(storageDirectoryConfiguration.storageLocations()).thenReturn(Map.of("image", tempDir.toString()));
		var siteFile = SiteFile.builder()
							   .fileName("gone.jpg")
							   .filePath("trip")
							   .fileType(FileTypeEnum.IMAGE)
							   .build();
		siteFile.setId(43L);
		siteFile.setAclId(11L);
		when(fileService.findSiteFileById(43L)).thenReturn(Optional.of(siteFile));
		when(fileService.findAllFileThumbsBySiteFileList(List.of(siteFile))).thenReturn(List.of());
		doThrow(new fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException())
				.when(aclService)
				.deleteByAclId(11L);

		fileIngestService.deleteIngestedSiteFile(43L);

		verify(siteFileRepository).delete(siteFile);
	}

	@Test
	void deleteIngestedSiteFile_unknownIdIsNotFound() {
		when(fileService.findSiteFileById(99L)).thenReturn(Optional.empty());

		var exception = assertThrows(org.springframework.web.server.ResponseStatusException.class,
									 () -> fileIngestService.deleteIngestedSiteFile(99L));

		assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.getStatusCode());
		verify(siteFileRepository, never()).delete(any(SiteFile.class));
	}
	// ─── additional ACL grantees ────────────────────────────────────────────────

	private static UserAccount activeUser(long id, String name) {
		return UserAccount.builder()
						  .id(id)
						  .name(name)
						  .loginName(name.toLowerCase())
						  .nick(name)
						  .status(AccountStatus.ACTIVE)
						  .locked(false)
						  .build();
	}

	private static FileIngestAclRequest aclFor(long userId, boolean read, boolean create, boolean modify, boolean delete) {
		return FileIngestAclRequest.builder()
								   .userId(userId)
								   .readPrivilege(read)
								   .createPrivilege(create)
								   .modifyPrivilege(modify)
								   .deletePrivilege(delete)
								   .build();
	}

	@Test
	void ingestInternal_withAcls_grantsThemOnTheNewSiteFileAndTheNewGallery() throws Exception {
		when(storageDirectoryConfiguration.storageLocations())
				.thenReturn(Map.of("image", tempDir.toString()));
		when(siteFileRepository.findByFilePathAndFileName(any(), any()))
				.thenReturn(Optional.empty());
		when(accessService.getUserId()).thenReturn(1L);
		when(userService.findById(5L)).thenReturn(Optional.of(activeUser(5L, "Five")));
		// First call creates the site file ACL, second the gallery ACL
		when(aclService.createNewAcl(anyLong(), isNull(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
				.thenReturn(10L, 20L);
		when(aclService.findAclByAclId(anyLong())).thenReturn(List.of());
		when(aclService.save(any(Acl.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(locationService.upsertAndGet(any())).thenReturn(null);

		var savedFile = SiteFile.builder()
		                        .build();
		savedFile.setId(42L);
		when(fileService.saveSiteFile(any(SiteFile.class))).thenReturn(savedFile);
		when(galleryRepository.findByShortname("NewGallery")).thenReturn(Optional.empty());
		var savedGallery = Gallery.builder()
		                          .id(99L)
		                          .aclId(20L)
		                          .shortname("NewGallery")
		                          .build();
		when(galleryRepository.save(any(Gallery.class))).thenReturn(savedGallery);
		when(galleryFileService.findGalleryFileByGalleryId(99L)).thenReturn(List.of());

		var multipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", FILE_CONTENT);
		var request = FileIngestRequest.builder()
									   .fileName("test.jpg")
									   .mimeType("image/jpeg")
									   .sha256sum(SHA256_SUM)
									   .comment("")
									   .metadata("{}")
									   .tags(List.of())
									   .galleryName("NewGallery")
									   .acls(List.of(aclFor(5L, true, false, true, false)))
									   .build();

		var response = fileIngestService.ingestInternal(request, multipartFile);

		assertEquals(42L, response.getSiteFileId());
		assertEquals(99L, response.getGalleryId());

		var captor = ArgumentCaptor.forClass(Acl.class);
		verify(aclService, times(2)).save(captor.capture());
		var rows = captor.getAllValues();
		assertEquals(List.of(10L, 20L), rows.stream()
		                                    .map(Acl::getAclId)
		                                    .toList());
		for (var row : rows) {
			assertEquals(5L, row.getUserId());
			assertNull(row.getUnitId());
			assertTrue(row.isReadPrivilege());
			assertFalse(row.isCreatePrivilege());
			assertTrue(row.isModifyPrivilege());
			assertFalse(row.isDeletePrivilege());
		}
		verify(aclService, never()).update(any());
	}

	@Test
	void ingestInternal_existingFile_mergesAclsIntoTheExistingAcl() throws Exception {
		when(storageDirectoryConfiguration.storageLocations())
				.thenReturn(Map.of("image", tempDir.toString()));
		var existingFile = SiteFile.builder()
		                           .build();
		existingFile.setId(7L);
		existingFile.setAclId(33L);
		when(siteFileRepository.findByFilePathAndFileName(any(), any()))
				.thenReturn(Optional.of(existingFile));
		when(accessService.getUserId()).thenReturn(1L);
		when(userService.findById(5L)).thenReturn(Optional.of(activeUser(5L, "Five")));
		when(userService.findById(6L)).thenReturn(Optional.of(activeUser(6L, "Six")));
		var existingRow = Acl.builder()
		                     .id(500L)
		                     .aclId(33L)
		                     .userId(5L)
		                     .readPrivilege(true)
		                     .build();
		var ownerRow = Acl.builder()
		                  .id(499L)
		                  .aclId(33L)
		                  .userId(1L)
		                  .readPrivilege(true)
		                  .createPrivilege(true)
		                  .modifyPrivilege(true)
						  .deletePrivilege(true)
		                  .build();
		when(aclService.findAclByAclId(33L)).thenReturn(List.of(ownerRow, existingRow));
		when(aclService.save(any(Acl.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(locationService.upsertAndGet(any())).thenReturn(null);
		when(fileService.saveSiteFile(any(SiteFile.class))).thenReturn(existingFile);

		var multipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", FILE_CONTENT);
		var request = FileIngestRequest.builder()
									   .fileName("test.jpg")
									   .mimeType("image/jpeg")
									   .sha256sum(SHA256_SUM)
									   .comment("")
									   .metadata("{}")
									   .tags(List.of())
									   .acls(List.of(aclFor(5L, true, false, true, false), aclFor(6L, true, false, false, false)))
									   .build();

		fileIngestService.ingestInternal(request, multipartFile);

		// User 5 already had a row: it is updated in place with the requested privileges
		verify(aclService).update(existingRow);
		assertTrue(existingRow.isModifyPrivilege());
		assertFalse(existingRow.isDeletePrivilege());
		// User 6 is new on the ACL
		var captor = ArgumentCaptor.forClass(Acl.class);
		verify(aclService).save(captor.capture());
		assertEquals(33L, captor.getValue()
		                        .getAclId());
		assertEquals(6L, captor.getValue()
		                       .getUserId());
		assertTrue(captor.getValue()
		                 .isReadPrivilege());
		// The owner row is left alone
		assertTrue(ownerRow.isDeletePrivilege());
		verify(aclService, never()).createNewAcl(anyLong(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
	}

	@Test
	void ingestInternal_unknownAclUser_isRejectedBeforeTheFileIsStored() throws Exception {
		when(userService.findById(404L)).thenReturn(Optional.empty());

		var multipartFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", FILE_CONTENT);
		var request = FileIngestRequest.builder()
									   .fileName("test.jpg")
									   .mimeType("image/jpeg")
									   .sha256sum(SHA256_SUM)
									   .comment("")
									   .metadata("{}")
									   .tags(List.of())
									   .acls(List.of(aclFor(404L, true, false, false, false)))
									   .build();

		var exception = assertThrows(VempainIngestException.class, () -> fileIngestService.ingestInternal(request, multipartFile));

		assertInstanceOf(IllegalArgumentException.class, exception.getCause());
		assertNull(exception.getStoredFile());
		try (var stored = Files.list(tempDir)) {
			assertEquals(0L, stored.count(), "nothing may be written before the ACL list is validated");
		}
		verify(storageDirectoryConfiguration, never()).storageLocations();
		verify(fileService, never()).saveSiteFile(any());
		verify(aclService, never()).save(any());
	}

	@Test
	void validateAcls_inactiveLockedDuplicateAndPrivilegelessEntriesAreRejected() {
		var disabled = activeUser(8L, "Eight");
		disabled.setStatus(AccountStatus.DISABLED);
		when(userService.findById(8L)).thenReturn(Optional.of(disabled));
		assertThrows(IllegalArgumentException.class, () -> fileIngestService.validateAcls(List.of(aclFor(8L, true, false, false, false))));

		var locked = activeUser(9L, "Nine");
		locked.setLocked(true);
		when(userService.findById(9L)).thenReturn(Optional.of(locked));
		assertThrows(IllegalArgumentException.class, () -> fileIngestService.validateAcls(List.of(aclFor(9L, true, false, false, false))));

		when(userService.findById(5L)).thenReturn(Optional.of(activeUser(5L, "Five")));
		assertThrows(IllegalArgumentException.class,
					 () -> fileIngestService.validateAcls(List.of(aclFor(5L, true, false, false, false), aclFor(5L, false, true, false, false))));

		assertThrows(IllegalArgumentException.class, () -> fileIngestService.validateAcls(List.of(aclFor(5L, false, false, false, false))));
		assertThrows(IllegalArgumentException.class, () -> fileIngestService.validateAcls(List.of(aclFor(0L, true, false, false, false))));
		assertThrows(IllegalArgumentException.class, () -> fileIngestService.validateAcls(java.util.Arrays.asList((FileIngestAclRequest) null)));

		// Valid lists pass without touching anything else
		fileIngestService.validateAcls(List.of(aclFor(5L, true, false, false, false)));
		fileIngestService.validateAcls(List.of());
		fileIngestService.validateAcls(null);
	}

	@Test
	void grantAdditionalAcls_skipsTheIngestingAccountAndEmptyInput() throws Exception {
		fileIngestService.grantAdditionalAcls(10L, List.of(aclFor(1L, true, true, true, true)), 1L);
		fileIngestService.grantAdditionalAcls(10L, List.of(), 1L);
		fileIngestService.grantAdditionalAcls(0L, List.of(aclFor(5L, true, false, false, false)), 1L);

		verify(aclService, never()).save(any());
		verify(aclService, never()).update(any());
	}

	@Test
	void listIngestUsers_returnsActiveUnlockedUsersSortedByNameWithReducedFields() {
		var zed = activeUser(3L, "Zed");
		zed.setEmail("zed@nohost.nodomain");
		var amy = activeUser(4L, "amy");
		var disabled = activeUser(5L, "Disabled");
		disabled.setStatus(AccountStatus.DISABLED);
		var locked = activeUser(6L, "Locked");
		locked.setLocked(true);
		when(userService.findAll()).thenReturn(List.of(zed, disabled, amy, locked));

		var users = fileIngestService.listIngestUsers();

		assertEquals(List.of(4L, 3L), users.stream()
		                                   .map(fi.poltsi.vempain.admin.api.response.file.FileIngestUserResponse::getId)
		                                   .toList());
		assertEquals("zed", users.get(1)
		                         .getLoginName());
		assertEquals("Zed", users.get(1)
		                         .getName());
		assertEquals("Zed", users.get(1)
		                         .getNick());
	}
}
