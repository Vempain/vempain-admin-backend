package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.api.PublishResultEnum;
import fi.poltsi.vempain.admin.api.response.PublishResponse;
import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.entity.Form;
import fi.poltsi.vempain.admin.entity.FormComponent;
import fi.poltsi.vempain.admin.entity.Layout;
import fi.poltsi.vempain.admin.entity.Page;
import fi.poltsi.vempain.admin.entity.PageGallery;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.admin.service.file.GalleryFileService;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.service.UserService;
import fi.poltsi.vempain.site.repository.WebGpsLocationRepository;
import fi.poltsi.vempain.site.repository.WebSiteFileRepository;
import fi.poltsi.vempain.site.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.site.repository.WebSitePageRepository;
import fi.poltsi.vempain.site.service.WebSiteResourceService;
import fi.poltsi.vempain.site.service.WebSiteSubjectService;
import fi.poltsi.vempain.tools.JschClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishServiceUTC {
	@Mock
	private SiteFileRepository       siteFileRepository;
	@Mock
	private WebSitePageRepository    webSitePageRepository;
	@Mock
	private WebSiteGalleryRepository webSiteGalleryRepository;
	@Mock
	private WebSiteFileRepository    webSiteFileRepository;
	@Mock
	private WebGpsLocationRepository webGpsLocationRepository;
	@Mock
	private PageService              pageService;
	@Mock
	private FormService              formService;
	@Mock
	private ComponentService         componentService;
	@Mock
	private FileService              fileService;
	@Mock
	private LayoutService            layoutService;
	@Mock
	private UserService              userService;
	@Mock
	private SubjectService           subjectService;
	@Mock
	private GalleryFileService    galleryFileService;
	@Mock
	private WebSiteSubjectService webSiteSubjectService;
	@Mock
	private PageGalleryService    pageGalleryService;
	@Mock
	private JschClient               jschClient;
	@Mock
	private WebSiteResourceService   webSiteResourceService;
	@Mock
	private AccessService            accessService;

	@InjectMocks
	private PublishService publishService;

	private PublishService publishServiceSpy;

	@BeforeEach
	void setupSpy() {
		publishServiceSpy = Mockito.spy(publishService);
	}

	@Test
	void publishSelectedGalleriesPublishesAllowedOnes() throws Exception {
		var gallery = Gallery.builder()
							 .id(1L)
							 .aclId(101L)
							 .build();
		when(fileService.findGalleryById(1L)).thenReturn(gallery);
		when(fileService.findGalleryById(2L)).thenReturn(null);
		when(accessService.hasModifyPermission(101L)).thenReturn(true);
		doNothing().when(publishServiceSpy)
				   .publishGalleryAsSystem(1L);

		PublishResponse response = publishServiceSpy.publishSelectedGalleries(List.of(1L, 2L));

		assertNotNull(response);
		assertEquals(PublishResultEnum.OK, response.getResult());
		assertEquals("Published 1 galleries, skipped 1", response.getMessage());
	}

	@Test
	void publishSelectedGalleriesReturnsFailWhenNonePublished() throws Exception {
		when(fileService.findGalleryById(anyLong())).thenReturn(null);

		PublishResponse response = publishServiceSpy.publishSelectedGalleries(List.of(5L));

		assertEquals(PublishResultEnum.FAIL, response.getResult());
	}

	@Test
	void publishSelectedGalleriesSkipsGalleryWithUnreadableFile() throws Exception {
		var gallery = Gallery.builder()
							 .id(3L)
							 .aclId(103L)
							 .siteFiles(List.of(SiteFile.builder()
														.id(30L)
														.aclId(301L)
														.build()))
							 .build();
		when(fileService.findGalleryById(3L)).thenReturn(gallery);
		when(accessService.hasModifyPermission(103L)).thenReturn(true);
		when(accessService.hasReadPermission(301L)).thenReturn(false);

		PublishResponse response = publishServiceSpy.publishSelectedGalleries(List.of(3L));

		assertEquals(PublishResultEnum.FAIL, response.getResult());
		verify(publishServiceSpy, never()).publishGalleryAsSystem(3L);
	}

	@Test
	void canPublishGalleryRequiresModifyOnGalleryAndReadOnEveryFile() {
		var files = List.of(SiteFile.builder()
		                            .id(1L)
		                            .aclId(11L)
		                            .build(), SiteFile.builder()
		                                              .id(2L)
		                                              .aclId(12L)
		                                              .build());
		var gallery = Gallery.builder()
		                     .id(5L)
		                     .aclId(50L)
		                     .siteFiles(files)
		                     .build();

		assertFalse(publishService.canPublishGallery((Gallery) null));

		when(accessService.hasModifyPermission(50L)).thenReturn(false);
		assertFalse(publishService.canPublishGallery(gallery));

		when(accessService.hasModifyPermission(50L)).thenReturn(true);
		when(accessService.hasReadPermission(11L)).thenReturn(true);
		when(accessService.hasReadPermission(12L)).thenReturn(false);
		assertFalse(publishService.canPublishGallery(gallery));

		when(accessService.hasReadPermission(12L)).thenReturn(true);
		assertTrue(publishService.canPublishGallery(gallery));

		gallery.setSiteFiles(null);
		assertTrue(publishService.canPublishGallery(gallery));
	}

	@Test
	void publishGalleryDeniesWhenUserMayNotPublish() {
		var gallery = Gallery.builder()
		                     .id(6L)
		                     .aclId(60L)
		                     .build();
		when(fileService.findGalleryById(6L)).thenReturn(gallery);
		when(accessService.hasModifyPermission(60L)).thenReturn(false);

		assertThrows(AccessDeniedException.class, () -> publishService.publishGallery(6L));
		verify(galleryFileService, never()).findGalleryFileByGalleryId(anyLong());
	}

	@Test
	void publishGalleryThrowsNotFoundForMissingGallery() {
		when(fileService.findGalleryById(7L)).thenReturn(null);

		assertThrows(VempainEntityNotFoundException.class, () -> publishService.publishGallery(7L));
	}

	@Test
	void canPublishPageChecksPageFormLayoutComponentsAndGalleries() throws Exception {
		var page = Page.builder()
		               .id(1L)
		               .aclId(10L)
		               .formId(2L)
		               .build();
		var form = Form.builder()
		               .id(2L)
		               .aclId(20L)
		               .layoutId(3L)
		               .build();
		var layout = Layout.builder()
		                   .id(3L)
		                   .aclId(30L)
		                   .build();
		var component = Component.builder()
		                         .id(4L)
		                         .aclId(40L)
		                         .build();
		var formComponent = FormComponent.builder()
		                                 .formId(2L)
		                                 .componentId(4L)
		                                 .sortOrder(0L)
		                                 .build();
		var pageGallery = PageGallery.builder()
		                             .pageId(1L)
		                             .galleryId(5L)
		                             .sortOrder(0L)
		                             .build();
		var gallery = Gallery.builder()
		                     .id(5L)
		                     .aclId(50L)
		                     .siteFiles(List.of(SiteFile.builder()
		                                                .id(9L)
		                                                .aclId(90L)
		                                                .build()))
		                     .build();

		assertFalse(publishService.canPublishPage(null));

		when(accessService.hasModifyPermission(10L)).thenReturn(false);
		assertFalse(publishService.canPublishPage(page));

		when(accessService.hasModifyPermission(10L)).thenReturn(true);
		when(formService.findById(2L)).thenReturn(form);
		when(accessService.hasReadPermission(20L)).thenReturn(false);
		assertFalse(publishService.canPublishPage(page));

		when(accessService.hasReadPermission(20L)).thenReturn(true);
		when(layoutService.findById(3L)).thenReturn(layout);
		when(accessService.hasReadPermission(30L)).thenReturn(false);
		assertFalse(publishService.canPublishPage(page));

		when(accessService.hasReadPermission(30L)).thenReturn(true);
		when(formService.findAllFormComponentsByFormId(2L)).thenReturn(List.of(formComponent));
		when(componentService.findById(4L)).thenReturn(component);
		when(accessService.hasReadPermission(40L)).thenReturn(false);
		assertFalse(publishService.canPublishPage(page));

		when(accessService.hasReadPermission(40L)).thenReturn(true);
		when(pageGalleryService.findPageGalleryByPageId(1L)).thenReturn(List.of(pageGallery));
		when(fileService.findGalleryById(5L)).thenReturn(gallery);
		when(accessService.hasModifyPermission(50L)).thenReturn(true);
		when(accessService.hasReadPermission(90L)).thenReturn(false);
		assertFalse(publishService.canPublishPage(page));

		when(accessService.hasReadPermission(90L)).thenReturn(true);
		assertTrue(publishService.canPublishPage(page));
	}

	@Test
	void canPublishPageSkipsMissingComponentsButFailsOnMissingForm() throws Exception {
		var page = Page.builder()
		               .id(1L)
		               .aclId(10L)
		               .formId(2L)
		               .build();
		var form = Form.builder()
		               .id(2L)
		               .aclId(20L)
		               .layoutId(3L)
		               .build();
		var layout = Layout.builder()
		                   .id(3L)
		                   .aclId(30L)
		                   .build();
		var formComponent = FormComponent.builder()
		                                 .formId(2L)
		                                 .componentId(4L)
		                                 .sortOrder(0L)
		                                 .build();
		when(accessService.hasModifyPermission(10L)).thenReturn(true);
		when(formService.findById(2L)).thenReturn(form);
		when(accessService.hasReadPermission(20L)).thenReturn(true);
		when(layoutService.findById(3L)).thenReturn(layout);
		when(accessService.hasReadPermission(30L)).thenReturn(true);
		when(formService.findAllFormComponentsByFormId(2L)).thenReturn(List.of(formComponent));
		when(componentService.findById(4L)).thenThrow(new VempainComponentException("missing"));
		when(pageGalleryService.findPageGalleryByPageId(1L)).thenReturn(List.of());

		assertTrue(publishService.canPublishPage(page));

		when(formService.findById(2L)).thenThrow(new VempainEntityNotFoundException("gone", "form"));
		assertFalse(publishService.canPublishPage(page));
	}

	@Test
	void publishPageDeniesWhenUserMayNotPublish() {
		var page = Page.builder()
		               .id(1L)
		               .aclId(10L)
		               .formId(2L)
		               .build();
		when(pageService.findById(1L)).thenReturn(page);
		when(accessService.hasModifyPermission(10L)).thenReturn(false);

		assertThrows(AccessDeniedException.class, () -> publishService.publishPage(1L));
		verify(webSitePageRepository, never()).save(any());
	}

	@Test
	void authorizePagePublishThrowsNotFoundForMissingPage() {
		when(pageService.findById(99L)).thenReturn(null);

		assertThrows(VempainEntityNotFoundException.class, () -> publishService.authorizePagePublish(99L));
	}

	@Test
	void publishAllPagesSkipsPagesTheUserMayNotPublish() throws Exception {
		var allowed = Page.builder()
		                  .id(1L)
		                  .aclId(10L)
		                  .formId(2L)
		                  .build();
		var denied = Page.builder()
		                 .id(2L)
		                 .aclId(20L)
		                 .formId(2L)
		                 .build();
		when(pageService.findAllByUser()).thenReturn(List.of(allowed, denied));
		doReturn(true).when(publishServiceSpy)
		              .canPublishPage(allowed);
		doReturn(false).when(publishServiceSpy)
		               .canPublishPage(denied);
		doReturn(1L).when(publishServiceSpy)
		            .publishPageAsSystem(1L);

		publishServiceSpy.publishAllPages();

		verify(publishServiceSpy).publishPageAsSystem(1L);
		verify(publishServiceSpy, never()).publishPageAsSystem(2L);
	}

	@Test
	void publishAllGalleriesSkipsGalleriesTheUserMayNotPublish() throws Exception {
		var allowed = Gallery.builder()
		                     .id(1L)
		                     .aclId(10L)
		                     .build();
		var denied = Gallery.builder()
		                    .id(2L)
		                    .aclId(20L)
		                    .build();
		when(fileService.findAllGalleries()).thenReturn(List.of(allowed, denied));
		doReturn(true).when(publishServiceSpy)
		              .canPublishGallery(1L);
		doReturn(false).when(publishServiceSpy)
		               .canPublishGallery(2L);
		doNothing().when(publishServiceSpy)
		           .publishGalleryAsSystem(1L);

		publishServiceSpy.publishAllGalleries();

		verify(publishServiceSpy).publishGalleryAsSystem(1L);
		verify(publishServiceSpy, never()).publishGalleryAsSystem(2L);
	}
}
