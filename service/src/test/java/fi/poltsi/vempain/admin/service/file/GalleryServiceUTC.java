package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.api.QueryDetailEnum;
import fi.poltsi.vempain.admin.api.request.file.GalleryRequest;
import fi.poltsi.vempain.admin.api.response.file.GalleryPageResponse;
import fi.poltsi.vempain.admin.api.response.file.GalleryResponse;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.service.AclService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GalleryServiceUTC {

	@Mock
	private GalleryRepository  galleryRepository;
	@Mock
	private SiteFileRepository siteFileRepository;
	@Mock
	private GalleryFileService galleryFileService;
	@Mock
	private AclService         aclService;
	@Mock
	private AccessService      accessService;

	@InjectMocks
	private GalleryService galleryService;

	private Gallery sampleGallery;

	@BeforeEach
	void setUp() {
		sampleGallery = Gallery.builder()
							   .id(1L)
							   .shortname("test-gallery")
							   .description("A test gallery")
							   .aclId(10L)
							   .creator(1L)
							   .created(Instant.now())
							   .build();
	}

	// ---- findAllForUser ----

	@Test
	void findAllForUserOk() {
		when(galleryRepository.findAll()).thenReturn(List.of(sampleGallery));
		when(accessService.hasReadPermission(10L)).thenReturn(true);
		when(aclService.findAclByAclId(10L)).thenReturn(List.of(Acl.builder().aclId(10L).build()));
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(Collections.emptyList());

		List<Gallery> result = galleryService.findAllForUser();

		assertNotNull(result);
		assertEquals(1, result.size());
	}

	@Test
	void findAllForUserNoPermissionExcludedOk() {
		when(galleryRepository.findAll()).thenReturn(List.of(sampleGallery));
		when(accessService.hasReadPermission(10L)).thenReturn(false);

		List<Gallery> result = galleryService.findAllForUser();

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void findAllForUserEmptyOk() {
		when(galleryRepository.findAll()).thenReturn(Collections.emptyList());

		List<Gallery> result = galleryService.findAllForUser();

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	// ---- findAllAsResponsesForUser ----

	@Test
	void findAllAsResponsesForUserShortDetailOk() {
		when(galleryRepository.findAll()).thenReturn(List.of(sampleGallery));
		when(accessService.hasReadPermission(10L)).thenReturn(true);
		when(aclService.findAclByAclId(10L)).thenReturn(List.of(Acl.builder().aclId(10L).build()));
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(Collections.emptyList());

		List<GalleryResponse> result = galleryService.findAllAsResponsesForUser(QueryDetailEnum.MINIMAL);

		assertNotNull(result);
		assertEquals(1, result.size());
	}

	@Test
	void findAllAsResponsesForUserFullDetailOk() {
		when(galleryRepository.findAll()).thenReturn(List.of(sampleGallery));
		when(accessService.hasReadPermission(10L)).thenReturn(true);
		when(aclService.findAclByAclId(10L)).thenReturn(List.of(Acl.builder().aclId(10L).build()));
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(Collections.emptyList());

		List<GalleryResponse> result = galleryService.findAllAsResponsesForUser(QueryDetailEnum.FULL);

		assertNotNull(result);
		assertEquals(1, result.size());
	}

	// ---- findById ----

	@Test
	void findByIdOk() {
		when(galleryRepository.findById(1L)).thenReturn(Optional.of(sampleGallery));
		when(accessService.hasReadPermission(10L)).thenReturn(true);
		when(aclService.findAclByAclId(10L)).thenReturn(List.of(Acl.builder().aclId(10L).build()));
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(Collections.emptyList());

		GalleryResponse result = galleryService.findById(1L);

		assertNotNull(result);
	}

	@Test
	void findByIdNotFoundReturnsNullOk() {
		when(galleryRepository.findById(99L)).thenReturn(Optional.empty());

		GalleryResponse result = galleryService.findById(99L);

		assertNull(result);
	}

	@Test
	void findByIdNoPermissionReturnsNullOk() {
		when(galleryRepository.findById(1L)).thenReturn(Optional.of(sampleGallery));
		when(accessService.hasReadPermission(10L)).thenReturn(false);

		GalleryResponse result = galleryService.findById(1L);

		assertNull(result);
	}

	// ---- createGallery ----

	@Test
	void createGalleryOk() throws VempainAclException {
		GalleryRequest request = GalleryRequest.builder()
											   .id(0L)
											   .shortName("new-gallery")
											   .description("New gallery")
											   .siteFilesId(new long[]{})
											   .acls(new ArrayList<>())
											   .build();
		when(aclService.getNextAclId()).thenReturn(20L);
		when(accessService.getUserId()).thenReturn(1L);
		when(galleryRepository.save(any(Gallery.class))).thenReturn(sampleGallery);
		doNothing().when(galleryFileService).addGalleryFiles(anyLong(), any(long[].class));
		doNothing().when(aclService).saveAclRequests(anyLong(), any());
		when(aclService.findAclByAclId(10L)).thenReturn(List.of(Acl.builder().aclId(10L).build()));
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(Collections.emptyList());

		GalleryResponse result = galleryService.createGallery(request);

		assertNotNull(result);
		verify(galleryRepository).save(any(Gallery.class));
	}

	@Test
	void createGalleryAclExceptionFail() throws VempainAclException {
		GalleryRequest request = GalleryRequest.builder()
											   .id(0L)
											   .shortName("bad-gallery")
											   .description("Bad")
											   .siteFilesId(new long[]{})
											   .acls(new ArrayList<>())
											   .build();
		when(aclService.getNextAclId()).thenReturn(20L);
		when(accessService.getUserId()).thenReturn(1L);
		when(galleryRepository.save(any(Gallery.class))).thenReturn(sampleGallery);
		doNothing().when(galleryFileService).addGalleryFiles(anyLong(), any(long[].class));
		doThrow(new RuntimeException("ACL error")).when(aclService).saveAclRequests(anyLong(), any());

		assertThrows(VempainAclException.class, () -> galleryService.createGallery(request));
	}

	// ---- updateGallery ----

	@Test
	void updateGalleryOk() throws VempainAclException {
		GalleryRequest request = GalleryRequest.builder()
											   .id(1L)
											   .shortName("updated-gallery")
											   .description("Updated description")
											   .siteFilesId(new long[]{})
											   .acls(new ArrayList<>())
											   .build();
		when(galleryRepository.findById(1L)).thenReturn(Optional.of(sampleGallery));
		when(accessService.getUserId()).thenReturn(1L);
		when(galleryRepository.save(any(Gallery.class))).thenReturn(sampleGallery);
		doNothing().when(galleryFileService).updateGalleryFiles(anyLong(), any(long[].class));
		doNothing().when(aclService).updateFromRequestList(any());
		when(aclService.findAclByAclId(10L)).thenReturn(List.of(Acl.builder().aclId(10L).build()));
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(Collections.emptyList());

		GalleryResponse result = galleryService.updateGallery(request);

		assertNotNull(result);
		verify(galleryRepository).save(any(Gallery.class));
	}

	@Test
	void updateGalleryNotFoundReturnsNullOk() throws VempainAclException {
		GalleryRequest request = GalleryRequest.builder()
											   .id(99L)
											   .shortName("ghost")
											   .description("Ghost")
											   .siteFilesId(new long[]{})
											   .build();
		when(galleryRepository.findById(99L)).thenReturn(Optional.empty());

		GalleryResponse result = galleryService.updateGallery(request);

		assertNull(result);
	}

	@Test
	void updateGalleryAclExceptionFail() throws VempainAclException {
		GalleryRequest request = GalleryRequest.builder()
											   .id(1L)
											   .shortName("err-gallery")
											   .description("Error")
											   .siteFilesId(new long[]{})
											   .acls(new ArrayList<>())
											   .build();
		when(galleryRepository.findById(1L)).thenReturn(Optional.of(sampleGallery));
		when(accessService.getUserId()).thenReturn(1L);
		when(galleryRepository.save(any(Gallery.class))).thenReturn(sampleGallery);
		doNothing().when(galleryFileService).updateGalleryFiles(anyLong(), any(long[].class));
		doThrow(new RuntimeException("ACL error")).when(aclService).updateFromRequestList(any());

		assertThrows(VempainAclException.class, () -> galleryService.updateGallery(request));
	}

	// ---- deleteGallery ----

	@Test
	void deleteGalleryOk() {
		doNothing().when(galleryRepository).deleteById(1L);

		galleryService.deleteGallery(1L);

		verify(galleryRepository).deleteById(1L);
	}

	// ---- searchGalleries / buildSort / populateGalleryWithSiteFiles ----

	@Test
	void searchGalleriesShortNameSortWithResultsOk() {
		var page = new PageImpl<>(List.of(sampleGallery));
		when(galleryRepository.searchGalleries(anyString(), anyBoolean(), any(Pageable.class))).thenReturn(page);
		when(accessService.hasReadPermission(10L)).thenReturn(true);
		when(aclService.findAclByAclId(10L)).thenReturn(List.of(Acl.builder().aclId(10L).build()));
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(Collections.emptyList());

		GalleryPageResponse result = galleryService.searchGalleries(0, 10, "short_name", "asc", "test", false);

		assertNotNull(result);
		assertEquals(1, result.getItems().size());
	}

	@Test
	void searchGalleriesShortnameSortDescOk() {
		var page = new PageImpl<>(List.of(sampleGallery));
		when(galleryRepository.searchGalleries(anyString(), anyBoolean(), any(Pageable.class))).thenReturn(page);
		when(accessService.hasReadPermission(10L)).thenReturn(true);
		when(aclService.findAclByAclId(10L)).thenReturn(List.of(Acl.builder().aclId(10L).build()));
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(Collections.emptyList());

		GalleryPageResponse result = galleryService.searchGalleries(0, 10, "shortname", "desc", "test", true);

		assertNotNull(result);
		assertEquals(1, result.getItems().size());
	}

	@Test
	void searchGalleriesDescriptionSortOk() {
		var page = new PageImpl<>(List.of(sampleGallery));
		when(galleryRepository.searchGalleries(anyString(), anyBoolean(), any(Pageable.class))).thenReturn(page);
		when(accessService.hasReadPermission(10L)).thenReturn(true);
		when(aclService.findAclByAclId(10L)).thenReturn(List.of(Acl.builder().aclId(10L).build()));
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(Collections.emptyList());

		GalleryPageResponse result = galleryService.searchGalleries(0, 10, "description", "asc", "", false);

		assertNotNull(result);
	}

	@Test
	void searchGalleriesDefaultSortNoPermissionOk() {
		var page = new PageImpl<>(List.of(sampleGallery));
		when(galleryRepository.searchGalleries(anyString(), anyBoolean(), any(Pageable.class))).thenReturn(page);
		when(accessService.hasReadPermission(10L)).thenReturn(false);

		GalleryPageResponse result = galleryService.searchGalleries(0, 10, null, "asc", "x", false);

		assertNotNull(result);
		assertTrue(result.getItems().isEmpty());
	}

	@Test
	void searchGalleriesWithSiteFilePopulatedOk() {
		var siteFile = SiteFile.builder().id(5L).creator(1L).build();
		var page = new PageImpl<>(List.of(sampleGallery));
		when(galleryRepository.searchGalleries(anyString(), anyBoolean(), any(Pageable.class))).thenReturn(page);
		when(accessService.hasReadPermission(10L)).thenReturn(true);
		when(aclService.findAclByAclId(10L)).thenReturn(List.of(Acl.builder().aclId(10L).build()));

		var galleryFile = fi.poltsi.vempain.admin.entity.file.GalleryFile.builder()
																		  .galleryId(1L)
																		  .siteFileId(5L)
																		  .build();
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(List.of(galleryFile));
		when(siteFileRepository.findByIdWithoutMetadata(5L)).thenReturn(Optional.of(siteFile));

		GalleryPageResponse result = galleryService.searchGalleries(0, 10, "id", "asc", "test", false);

		assertNotNull(result);
		assertEquals(1, result.getItems().size());
	}

	// ---- findAllAsResponsesForUser full detail (populateGalleryWithSiteFiles withMetadata=true) ----

	@Test
	void findAllAsResponsesForUserFullDetailWithSiteFilesOk() {
		var siteFile = SiteFile.builder().id(5L).creator(1L).build();
		when(galleryRepository.findAll()).thenReturn(List.of(sampleGallery));
		when(accessService.hasReadPermission(10L)).thenReturn(true);
		when(aclService.findAclByAclId(10L)).thenReturn(List.of(Acl.builder().aclId(10L).build()));

		var galleryFile = fi.poltsi.vempain.admin.entity.file.GalleryFile.builder()
																		  .galleryId(1L)
																		  .siteFileId(5L)
																		  .build();
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(List.of(galleryFile));
		when(siteFileRepository.findById(5L)).thenReturn(Optional.of(siteFile));

		List<GalleryResponse> result = galleryService.findAllAsResponsesForUser(QueryDetailEnum.FULL);

		assertNotNull(result);
		assertEquals(1, result.size());
	}

	// ---- findAll ----

	@Test
	void findAllOk() {
		when(galleryRepository.findAll()).thenReturn(List.of(sampleGallery));

		Iterable<Gallery> result = galleryService.findAll();

		assertNotNull(result);
		assertTrue(result.iterator().hasNext());
	}

}
