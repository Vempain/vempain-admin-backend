package fi.poltsi.vempain.site.service;

import fi.poltsi.vempain.admin.api.site.WebSiteResourceEnum;
import fi.poltsi.vempain.admin.api.site.request.WebSiteResourcePagedRequest;
import fi.poltsi.vempain.admin.api.site.response.WebSiteResourceResponse;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.common.api.FileTypeEnum;
import fi.poltsi.vempain.site.entity.WebSiteFile;
import fi.poltsi.vempain.site.entity.WebSiteGallery;
import fi.poltsi.vempain.site.entity.WebSitePage;
import fi.poltsi.vempain.site.repository.WebSiteFileRepository;
import fi.poltsi.vempain.site.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.site.repository.WebSitePageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WebSiteResourceService} focusing on routing logic, sorting fallback,
 * ACL + query precedence, and gallery/page query fallbacks.
 */
class WebSiteResourceServiceUTC {
	@Mock
	private WebSiteFileRepository  fileRepo;
	@Mock
	private WebSitePageRepository    pageRepo;
	@Mock
	private WebSiteGalleryRepository galleryRepo;
	@Mock
	private AccessService            accessService;
	@InjectMocks
	private WebSiteResourceService service;

	private AutoCloseable mocks;

	@BeforeEach
	void setUp() {
		mocks = MockitoAnnotations.openMocks(this);
		// Allow authentication check to pass silently
		doNothing().when(accessService)
				   .checkAuthentication();
	}

	@Test
	@DisplayName("Null type lists all resource kinds and sorts by id")
	void listResources_defaultsToAllTypesWhenTypeNull() {
		var file = WebSiteFile.builder()
							  .id(10L)
							  .aclId(5L)
							  .filePath("a/b/c.jpg")
							  .fileType(FileTypeEnum.IMAGE)
							  .build();
		var gallery = WebSiteGallery.builder()
									.id(20L)
									.aclId(6L)
									.shortname("Gallery")
									.description("Desc")
									.build();
		var page = WebSitePage.builder()
							  .id(30L)
							  .aclId(7L)
							  .title("Page title")
							  .filePath("/page")
							  .build();

		when(fileRepo.findAll(any(PageRequest.class))).thenAnswer(inv -> new PageImpl<>(List.of(file), inv.getArgument(0), 1));
		when(galleryRepo.findAll(any(PageRequest.class))).thenAnswer(inv -> new PageImpl<>(List.of(gallery), inv.getArgument(0), 1));
		when(pageRepo.findAll(any(PageRequest.class))).thenAnswer(inv -> new PageImpl<>(List.of(page), inv.getArgument(0), 1));

		var resp = service.listResources(request(null, null, null, null, null, null, 0, 10));
		assertThat(resp.getContent()).hasSize(3);
		assertThat(resp.getContent()).extracting(WebSiteResourceResponse::getResourceType)
								   .containsExactlyInAnyOrder(WebSiteResourceEnum.SITE_FILE, WebSiteResourceEnum.GALLERY, WebSiteResourceEnum.PAGE);
		assertThat(resp.getPage()).isEqualTo(0);
	}

	@Test
	@DisplayName("Unsupported sort field falls back to id")
	void listResources_invalidSortFallsBackToId() {
		var file = WebSiteFile.builder()
							  .id(2L)
							  .aclId(5L)
							  .filePath("x/y/z.png")
							  .fileType(FileTypeEnum.IMAGE)
							  .build();
		when(fileRepo.findAll(any(PageRequest.class))).thenAnswer(inv -> {
			PageRequest pr = inv.getArgument(0);
			// Expect sort field corrected to 'id'
			assertThat(pr.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
			return new PageImpl<>(List.of(file), pr, 1);
		});

		service.listResources(request(WebSiteResourceEnum.SITE_FILE, null, null, null, "non_existing_field", "asc", 0, 5));
	}

	@Test
	@DisplayName("ACL + fileType + query precedence for SITE_FILE")
	void listResources_aclFilteringFileTypeAndQuery() {
		var file = WebSiteFile.builder()
							  .id(3L)
							  .aclId(99L)
							  .filePath("filtered/file.png")
							  .fileType(FileTypeEnum.IMAGE)
							  .build();
		when(fileRepo.findByAclIdAndFileTypeAndFilePathContainingIgnoreCase(eq(99L), eq(FileTypeEnum.IMAGE), eq("filtered"), any()))
				.thenAnswer(inv -> new PageImpl<>(List.of(file), inv.getArgument(3), 1));

		var resp = service.listResources(request(WebSiteResourceEnum.SITE_FILE, FileTypeEnum.IMAGE, "filtered", 99L, "id", "desc", 0, 25));
		assertThat(resp.getContent()).extracting("aclId")
								   .containsExactly(99L);
		assertThat(resp.getContent()
					   .getFirst()
					   .getFileType()).isEqualTo("image");
	}

	@Test
	@DisplayName("Page title fallback to path when title search empty")
	void listResources_pageTitleFallbackPath() {
		when(pageRepo.findByAclIdAndTitleContainingIgnoreCase(anyLong(), anyString(), any()))
				.thenReturn(new PageImpl<>(List.of())); // empty title match
		var pageEntity = WebSitePage.builder()
									.id(7L)
									.aclId(11L)
									.title("Title X")
									.filePath("/x/path")
									.build();
		when(pageRepo.findByAclIdAndFilePathContainingIgnoreCase(anyLong(), anyString(), any()))
				.thenAnswer(inv -> new PageImpl<>(List.of(pageEntity), inv.getArgument(2), 1));

		var resp = service.listResources(request(WebSiteResourceEnum.PAGE, null, "search", 11L, "title", "asc", 0, 10));
		assertThat(resp.getContent()).hasSize(1);
		assertThat(resp.getContent()
					   .getFirst()
					   .getPath()).isEqualTo("/x/path");
	}

	@Test
	@DisplayName("Gallery shortname fallback to description when shortname search empty")
	void listResources_galleryShortnameFallbackDescription() {
		when(galleryRepo.findByAclIdAndShortnameContainingIgnoreCase(anyLong(), anyString(), any()))
				.thenReturn(new PageImpl<>(List.of()));
		var gallery = WebSiteGallery.builder()
									.id(4L)
									.aclId(22L)
									.shortname("Short")
									.description("Desc AAA")
									.build();
		when(galleryRepo.findByAclIdAndDescriptionContainingIgnoreCase(anyLong(), anyString(), any()))
				.thenAnswer(inv -> new PageImpl<>(List.of(gallery), inv.getArgument(2), 1));

		var resp = service.listResources(request(WebSiteResourceEnum.GALLERY, null, "aaa", 22L, "shortname", "asc", 0, 10));
		assertThat(resp.getContent()).hasSize(1);
		assertThat(resp.getContent()
					   .getFirst()
					   .getName()).isEqualTo("Short");
	}

	private WebSiteResourcePagedRequest request(WebSiteResourceEnum type, FileTypeEnum fileType, String search, Long aclId,
	                                            String sortBy, String direction, int page, int size) {
		var request = new WebSiteResourcePagedRequest();
		request.setType(type);
		request.setFileType(fileType);
		request.setSearch(search);
		request.setAclId(aclId);
		request.setSortBy(sortBy);
		request.setDirection("desc".equalsIgnoreCase(direction)
		                     ? Sort.Direction.DESC
		                     : Sort.Direction.ASC);
		request.setPage(page);
		request.setSize(size);
		return request;
	}

	// ---- sort field mapping, ACL and query combinations of every type, the merged listing edge cases ----

	private static WebSiteResourcePagedRequest pagedRequest(WebSiteResourceEnum type, FileTypeEnum fileType, String search, Long aclId, String sortBy,
															Sort.Direction direction, int page, int size) {
		var request = new WebSiteResourcePagedRequest();
		request.setType(type);
		request.setFileType(fileType);
		request.setSearch(search);
		request.setAclId(aclId);
		request.setSortBy(sortBy);
		request.setDirection(direction);
		request.setPage(page);
		request.setSize(size);
		return request;
	}

	private static <T> PageImpl<T> pageOf(List<T> content) {
		return new PageImpl<>(content, PageRequest.of(0, 10), content.size());
	}

	@Test
	void theVirtualNameSortMapsToTheNameColumnOfEachType() {
		when(fileRepo.findAll(any(Pageable.class))).thenReturn(pageOf(List.<WebSiteFile>of()));
		when(pageRepo.findAll(any(Pageable.class))).thenReturn(pageOf(List.<WebSitePage>of()));
		when(galleryRepo.findAll(any(Pageable.class))).thenReturn(pageOf(List.<WebSiteGallery>of()));

		service.listResources(pagedRequest(WebSiteResourceEnum.SITE_FILE, null, null, null, "name", null, 0, 10));
		service.listResources(pagedRequest(WebSiteResourceEnum.PAGE, null, null, null, "name", null, 0, 10));
		service.listResources(pagedRequest(WebSiteResourceEnum.GALLERY, null, null, null, "name", null, 0, 10));
		service.listResources(pagedRequest(WebSiteResourceEnum.SITE_FILE, null, null, null, "acl_id", Sort.Direction.DESC, 0, 10));
		service.listResources(pagedRequest(WebSiteResourceEnum.SITE_FILE, null, null, null, "file_type", null, 0, 10));

		verify(fileRepo).findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "path")));
		verify(pageRepo).findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "title")));
		verify(galleryRepo).findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "shortname")));
		verify(fileRepo).findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "aclId")));
		verify(fileRepo).findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "fileType")));
	}

	@Test
	void fileQueriesCombineAclFileTypeAndSearchInEveryWay() {
		var file = WebSiteFile.builder()
							  .id(1L)
							  .aclId(5L)
							  .filePath("a.jpg")
							  .build();
		when(fileRepo.findByAclIdAndFileType(eq(5L), eq(FileTypeEnum.IMAGE), any(Pageable.class))).thenReturn(pageOf(List.of(file)));
		when(fileRepo.findByAclIdAndFilePathContainingIgnoreCase(eq(5L), eq("trip"), any(Pageable.class))).thenReturn(pageOf(List.of(file)));
		when(fileRepo.findByFileTypeAndFilePathContainingIgnoreCase(eq(FileTypeEnum.IMAGE), eq("trip"), any(Pageable.class))).thenReturn(pageOf(List.of(file)));
		when(fileRepo.findByFileType(eq(FileTypeEnum.IMAGE), any(Pageable.class))).thenReturn(pageOf(List.of(file)));
		when(fileRepo.findByFilePathContainingIgnoreCase(eq("trip"), any(Pageable.class))).thenReturn(pageOf(List.of(file)));

		assertThat(service.listResources(pagedRequest(WebSiteResourceEnum.SITE_FILE, FileTypeEnum.IMAGE, null, 5L, null, null, 0, 10))
						  .getContent()).hasSize(1);
		assertThat(service.listResources(pagedRequest(WebSiteResourceEnum.SITE_FILE, null, "trip", 5L, null, null, 0, 10))
						  .getContent()).hasSize(1);
		assertThat(service.listResources(pagedRequest(WebSiteResourceEnum.SITE_FILE, FileTypeEnum.IMAGE, "trip", null, null, null, 0, 10))
						  .getContent()).hasSize(1);
		assertThat(service.listResources(pagedRequest(WebSiteResourceEnum.SITE_FILE, FileTypeEnum.IMAGE, null, null, null, null, 0, 10))
						  .getContent()).hasSize(1);
		var byPath = service.listResources(pagedRequest(WebSiteResourceEnum.SITE_FILE, null, "trip", null, null, null, 0, 10));
		assertThat(byPath.getContent()).hasSize(1);
		// A file without a type answers a null file type
		assertThat(byPath.getContent()
						 .getFirst()
						 .getFileType()).isNull();
	}

	@Test
	void pageAndGalleryQueriesAreRestrictedToTheAclWhenOneIsGiven() {
		var page = WebSitePage.builder()
							  .id(1L)
							  .aclId(5L)
							  .title("Trip")
							  .build();
		var gallery = WebSiteGallery.builder()
									.id(2L)
									.aclId(5L)
									.shortname("trip")
									.build();
		when(pageRepo.findByAclIdAndTitleContainingIgnoreCase(eq(5L), anyString(), any(Pageable.class))).thenReturn(pageOf(List.of(page)));
		when(pageRepo.findByAclIdAndFilePathContainingIgnoreCase(eq(5L), anyString(), any(Pageable.class))).thenReturn(pageOf(List.<WebSitePage>of()));
		when(pageRepo.findByAclIdAndBodyContainingIgnoreCase(eq(5L), anyString(), any(Pageable.class))).thenReturn(pageOf(List.<WebSitePage>of()));
		when(pageRepo.findByAclIdAndHeaderContainingIgnoreCase(eq(5L), anyString(), any(Pageable.class))).thenReturn(null);
		when(galleryRepo.findByAclIdAndShortnameContainingIgnoreCase(eq(5L), anyString(), any(Pageable.class))).thenReturn(pageOf(List.of(gallery)));
		when(galleryRepo.findByAclIdAndDescriptionContainingIgnoreCase(eq(5L), anyString(), any(Pageable.class))).thenReturn(pageOf(List.of(gallery)));
		when(pageRepo.findByAclId(eq(5L), any(Pageable.class))).thenReturn(pageOf(List.of(page)));
		when(galleryRepo.findByAclId(eq(5L), any(Pageable.class))).thenReturn(pageOf(List.of(gallery)));

		assertThat(service.listResources(pagedRequest(WebSiteResourceEnum.PAGE, null, "\"trip report\" 2024", 5L, null, null, 0, 10))
						  .getContent()).hasSize(1);
		assertThat(service.listResources(pagedRequest(WebSiteResourceEnum.GALLERY, null, "trip", 5L, null, null, 0, 10))
						  .getContent()).hasSize(1);
		// A blank query lists by ACL only
		assertThat(service.listResources(pagedRequest(WebSiteResourceEnum.PAGE, null, "   ", 5L, null, null, 0, 10))
						  .getContent()).hasSize(1);
		assertThat(service.listResources(pagedRequest(WebSiteResourceEnum.GALLERY, null, null, 5L, null, null, 0, 10))
						  .getContent()).hasSize(1);
	}

	@Test
	void theMergedListingHandlesNoResultsPagesPastTheEndAndOverflowingPages() {
		when(fileRepo.findAll(any(Pageable.class))).thenReturn(pageOf(List.<WebSiteFile>of()));
		when(pageRepo.findAll(any(Pageable.class))).thenReturn(pageOf(List.<WebSitePage>of()));
		when(galleryRepo.findAll(any(Pageable.class))).thenReturn(pageOf(List.<WebSiteGallery>of()));

		var empty = service.listResources(pagedRequest(null, null, null, null, null, null, 0, 10));
		assertThat(empty.getContent()).isEmpty();
		assertThat(empty.getTotalElements()).isZero();

		var file = WebSiteFile.builder()
							  .id(1L)
							  .aclId(5L)
							  .filePath("a.jpg")
							  .build();
		when(fileRepo.findAll(any(Pageable.class))).thenReturn(pageOf(List.of(file)));
		var pastTheEnd = service.listResources(pagedRequest(null, null, null, null, null, null, 3, 10));
		assertThat(pastTheEnd.getContent()).isEmpty();
		assertThat(pastTheEnd.getTotalElements()).isEqualTo(1);
		assertThat(pastTheEnd.getTotalPages()).isEqualTo(1);
		assertThat(pastTheEnd.getContent()).isEmpty();

		assertThrows(IllegalArgumentException.class, () -> service.listResources(pagedRequest(null, null, null, null, null, null, Integer.MAX_VALUE, 200)));
	}
}
