package fi.poltsi.vempain.site.service;

import fi.poltsi.vempain.admin.api.site.WebSiteResourceEnum;
import fi.poltsi.vempain.admin.api.site.response.WebSiteResourcePageResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteResourceResponse;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.file.api.FileTypeEnum;
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
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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

		WebSiteResourcePageResponse resp = service.listResources(null, null, null, null, null, null, 0, 10);
		assertThat(resp.getItems()).hasSize(3);
		assertThat(resp.getItems()).extracting(WebSiteResourceResponse::getResourceType)
								   .containsExactlyInAnyOrder(WebSiteResourceEnum.SITE_FILE, WebSiteResourceEnum.GALLERY, WebSiteResourceEnum.PAGE);
		assertThat(resp.getPageNumber()).isEqualTo(0);
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

		service.listResources(WebSiteResourceEnum.SITE_FILE, null, null, null, "non_existing_field", "asc", 0, 5);
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

		WebSiteResourcePageResponse resp = service.listResources(WebSiteResourceEnum.SITE_FILE, FileTypeEnum.IMAGE, "filtered", 99L, "id", "desc", 0, 25);
		assertThat(resp.getItems()).extracting("aclId")
								   .containsExactly(99L);
		assertThat(resp.getItems()
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

		WebSiteResourcePageResponse resp = service.listResources(WebSiteResourceEnum.PAGE, null, "search", 11L, "title", "asc", 0, 10);
		assertThat(resp.getItems()).hasSize(1);
		assertThat(resp.getItems()
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

		WebSiteResourcePageResponse resp = service.listResources(WebSiteResourceEnum.GALLERY, null, "aaa", 22L, "shortname", "asc", 0, 10);
		assertThat(resp.getItems()).hasSize(1);
		assertThat(resp.getItems()
					   .getFirst()
					   .getName()).isEqualTo("Short");
	}
}
