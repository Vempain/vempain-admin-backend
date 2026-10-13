package fi.poltsi.vempain.admin.service;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import fi.poltsi.vempain.admin.api.PublishResultEnum;
import fi.poltsi.vempain.admin.api.response.PublishResponse;
import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.entity.Form;
import fi.poltsi.vempain.admin.entity.FormComponent;
import fi.poltsi.vempain.admin.entity.Layout;
import fi.poltsi.vempain.admin.entity.Page;
import fi.poltsi.vempain.admin.entity.PageGallery;
import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.admin.entity.file.FileThumb;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.entity.file.GalleryFile;
import fi.poltsi.vempain.admin.entity.file.GpsLocation;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.admin.service.file.GalleryFileService;
import fi.poltsi.vempain.auth.api.response.UserResponse;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.service.UserService;
import fi.poltsi.vempain.common.api.FileTypeEnum;
import fi.poltsi.vempain.common.api.TaskStatusEnum;
import fi.poltsi.vempain.common.task.TaskProgress;
import fi.poltsi.vempain.common.task.TaskProgressStore;
import fi.poltsi.vempain.common.task.TaskRunner;
import fi.poltsi.vempain.site.entity.WebGpsLocation;
import fi.poltsi.vempain.site.entity.WebSiteFile;
import fi.poltsi.vempain.site.entity.WebSiteGallery;
import fi.poltsi.vempain.site.entity.WebSitePage;
import fi.poltsi.vempain.site.entity.WebSiteSubject;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
	@Mock
	private ApplicationContext applicationContext;

	// A synchronous task runner: submitted work runs on the calling thread, so the task state can be asserted right away
	private final TaskProgressStore taskStore  = new TaskProgressStore();
	@Spy
	private       TaskRunner        taskRunner = new TaskRunner(taskStore, Runnable::run);

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
	void publishPageDoesNotAddLegacyPhpWrapper() throws Exception {
		var page = Page.builder()
					   .id(1L)
					   .formId(2L)
					   .body("Page body")
					   .pagePath("/page")
					   .title("Page")
					   .header("Header")
					   .creator(7L)
					   .aclId(1L)
					   .build();
		var form = Form.builder()
					   .id(2L)
					   .layoutId(3L)
					   .build();
		var layout = Layout.builder()
						   .id(3L)
						   .structure("<main><!--page--></main>")
						   .build();

		when(pageService.findById(1L)).thenReturn(page);
		when(formService.findById(2L)).thenReturn(form);
		when(formService.findAllFormComponentsByFormId(2L)).thenReturn(List.of());
		when(layoutService.findById(3L)).thenReturn(layout);
		when(userService.findUserResponseById(7L)).thenReturn(UserResponse.builder()
		                                                                  .nick("creator")
		                                                                  .build());
		when(pageGalleryService.findPageGalleryByPageId(1L)).thenReturn(List.of());
		when(webSitePageRepository.findByPageId(1L)).thenReturn(java.util.Optional.empty());
		when(webSitePageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		publishService.publishPageAsSystem(1L);

		verify(webSitePageRepository).save(argThat(sitePage -> "<main>Page body</main>".equals(sitePage.getBody())));
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

	// ---------------------------------------------------------------- background tasks

	@Test
	void publishPageAsTaskIsDeniedBeforeAnyTaskIsSubmitted() {
		var page = Page.builder()
					   .id(1L)
					   .aclId(10L)
					   .formId(2L)
					   .build();
		when(pageService.findById(1L)).thenReturn(page);
		when(accessService.hasModifyPermission(10L)).thenReturn(false);

		assertThrows(AccessDeniedException.class, () -> publishService.publishPageAsTask(1L));

		verify(taskRunner, never()).submitDurable(any(), any(), anyLong(), any(), any());
		assertEquals(0, taskStore.size());
	}

	@Test
	void publishPageAsTaskRunsThePublishThroughTheTransactionalProxy() throws Exception {
		var page = Page.builder()
					   .id(1L)
					   .aclId(10L)
					   .formId(2L)
					   .title("Front page")
					   .build();
		when(pageService.findById(1L)).thenReturn(page);
		doReturn(true).when(publishServiceSpy)
					  .canPublishPage(page);
		doReturn(77L).when(publishServiceSpy)
					 .publishPageAsSystem(org.mockito.ArgumentMatchers.eq(1L), any());
		when(applicationContext.getBean(PublishService.class)).thenReturn(publishServiceSpy);

		var accepted = publishServiceSpy.publishPageAsTask(1L);

		assertEquals("PUBLISH_PAGE", accepted.getType());
		assertEquals("Publish page Front page", accepted.getTitle());
		var task = taskStore.find(accepted.getTaskId())
							.orElseThrow();
		assertEquals(TaskStatusEnum.COMPLETED, task.getStatus());
		assertEquals(Map.of("site_page_id", 77L), task.getResult());
		assertEquals(100, task.percent());
		verify(publishServiceSpy).publishPageAsSystem(org.mockito.ArgumentMatchers.eq(1L), any());
	}

	@Test
	void publishAllPagesAsTaskSkipsPagesTheUserMayNotPublish() throws Exception {
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
					.publishPageAsSystem(org.mockito.ArgumentMatchers.eq(1L), any());
		when(applicationContext.getBean(PublishService.class)).thenReturn(publishServiceSpy);

		var accepted = publishServiceSpy.publishAllPagesAsTask();

		assertEquals(1, accepted.getTotalSteps());
		var task = taskStore.find(accepted.getTaskId())
							.orElseThrow();
		assertEquals(TaskStatusEnum.COMPLETED, task.getStatus());
		assertEquals(Map.of("published", 1L, "skipped", 0L), task.getResult());
		verify(publishServiceSpy).publishPageAsSystem(org.mockito.ArgumentMatchers.eq(1L), any());
		verify(publishServiceSpy, never()).publishPageAsSystem(org.mockito.ArgumentMatchers.eq(2L), any());
		verify(webSitePageRepository).resetCache();
	}

	@Test
	void publishPagesNowCountsAMissingPageAsAFailedStepWithoutFailingTheTask() throws Exception {
		Mockito.doAnswer(invocation -> {
				   invocation.getArgument(1, TaskProgress.class)
							 .advance("Published page 1");
				   return 1L;
			   })
			   .when(publishServiceSpy)
			   .publishPageAsSystem(org.mockito.ArgumentMatchers.eq(1L), any());
		Mockito.doThrow(new VempainEntityNotFoundException("Page not found by id: 2", "page"))
			   .when(publishServiceSpy)
			   .publishPageAsSystem(org.mockito.ArgumentMatchers.eq(2L), any());
		when(applicationContext.getBean(PublishService.class)).thenReturn(publishServiceSpy);
		var progress = taskStore.create("PUBLISH_ALL_PAGES", "Publish all pages", 1L, 2);

		var result = publishServiceSpy.publishPagesNow(List.of(1L, 2L), progress);

		assertEquals(Map.of("published", 1L, "skipped", 1L), result);
		assertEquals(1, progress.getFailedSteps()
								.get());
		assertEquals(2, progress.getCompletedSteps()
								.get());
	}

	@Test
	void publishSelectedGalleriesAsTaskSkipsUnpublishableGalleriesAtSubmitTime() throws Exception {
		var allowed = Gallery.builder()
							 .id(1L)
							 .aclId(101L)
							 .build();
		var denied = Gallery.builder()
							.id(3L)
							.aclId(103L)
							.build();
		when(fileService.findGalleryById(1L)).thenReturn(allowed);
		when(fileService.findGalleryById(2L)).thenReturn(null);
		when(fileService.findGalleryById(3L)).thenReturn(denied);
		when(accessService.hasModifyPermission(101L)).thenReturn(true);
		when(accessService.hasModifyPermission(103L)).thenReturn(false);
		doNothing().when(publishServiceSpy)
				   .publishGalleryAsSystem(org.mockito.ArgumentMatchers.eq(1L), any());
		when(applicationContext.getBean(PublishService.class)).thenReturn(publishServiceSpy);

		var accepted = publishServiceSpy.publishSelectedGalleriesAsTask(List.of(1L, 2L, 3L, 0L));

		assertEquals("PUBLISH_SELECTED_GALLERIES", accepted.getType());
		assertEquals(1, accepted.getTotalSteps());
		var task = taskStore.find(accepted.getTaskId())
							.orElseThrow();
		assertEquals(TaskStatusEnum.COMPLETED, task.getStatus());
		assertEquals(Map.of("published", 1L, "skipped", 3L), task.getResult());
		verify(publishServiceSpy).publishGalleryAsSystem(org.mockito.ArgumentMatchers.eq(1L), any());
		verify(publishServiceSpy, never()).publishGalleryAsSystem(org.mockito.ArgumentMatchers.eq(3L), any());
	}

	@Test
	void publishSelectedGalleriesAsTaskRejectsAnEmptyList() {
		assertThrows(ResponseStatusException.class, () -> publishService.publishSelectedGalleriesAsTask(List.of()));
		assertEquals(0, taskStore.size());
	}

	@Test
	void publishGalleryAsTaskVerifiesThePrivilegesBeforeSubmitting() {
		var gallery = Gallery.builder()
							 .id(4L)
							 .aclId(104L)
							 .build();
		when(fileService.findGalleryById(4L)).thenReturn(gallery);
		when(accessService.hasModifyPermission(104L)).thenReturn(false);

		assertThrows(AccessDeniedException.class, () -> publishService.publishGalleryAsTask(4L));
		assertEquals(0, taskStore.size());
	}

	@Test
	void publishGalleryAsTaskCountsOneStepPerTransferredFile() throws Exception {
		var gallery = Gallery.builder()
							 .id(1L)
							 .aclId(101L)
							 .shortname("trip")
							 .siteFiles(List.of(SiteFile.builder()
														.id(11L)
														.aclId(111L)
														.fileName("a.jpg")
														.build(),
												SiteFile.builder()
														.id(12L)
														.aclId(112L)
														.fileName("b.jpg")
														.build(),
												SiteFile.builder()
														.id(13L)
														.aclId(113L)
														.fileName("c.jpg")
														.build()))
							 .build();
		when(fileService.findGalleryById(1L)).thenReturn(gallery);
		when(accessService.hasModifyPermission(101L)).thenReturn(true);
		when(accessService.hasReadPermission(anyLong())).thenReturn(true);
		Mockito.doAnswer(invocation -> {
				   var progress = invocation.getArgument(1, TaskProgress.class);
				   gallery.getSiteFiles()
						  .forEach(file -> progress.advance("Transferred " + file.getFileName()));
				   return null;
			   })
			   .when(publishServiceSpy)
			   .publishGalleryAsSystem(org.mockito.ArgumentMatchers.eq(1L), any());
		when(applicationContext.getBean(PublishService.class)).thenReturn(publishServiceSpy);

		var accepted = publishServiceSpy.publishGalleryAsTask(1L);

		assertEquals(3, accepted.getTotalSteps(), "the total is the number of files, known when the task is submitted");
		var task = taskStore.find(accepted.getTaskId())
							.orElseThrow();
		assertEquals(TaskStatusEnum.COMPLETED, task.getStatus());
		assertEquals(3, task.getCompletedSteps()
							.get());
		assertEquals(0, task.getFailedSteps()
							.get());
		assertEquals(Map.of("published", 1L, "skipped", 0L), task.getResult());
	}

	@Test
	void publishPageAsTaskCountsThePageAndTheFilesOfItsGalleries() throws Exception {
		var page = Page.builder()
					   .id(1L)
					   .aclId(10L)
					   .formId(2L)
					   .title("Trip report")
					   .build();
		var gallery = Gallery.builder()
							 .id(5L)
							 .aclId(50L)
							 .siteFiles(List.of(SiteFile.builder()
														.id(51L)
														.aclId(511L)
														.build(),
												SiteFile.builder()
														.id(52L)
														.aclId(512L)
														.build()))
							 .build();
		when(pageService.findById(1L)).thenReturn(page);
		when(pageGalleryService.findPageGalleryByPageId(1L)).thenReturn(List.of(PageGallery.builder()
																						   .pageId(1L)
																						   .galleryId(5L)
																						   .build()));
		when(fileService.findGalleryById(5L)).thenReturn(gallery);
		doReturn(true).when(publishServiceSpy)
					  .canPublishPage(page);
		doReturn(77L).when(publishServiceSpy)
					 .publishPageAsSystem(org.mockito.ArgumentMatchers.eq(1L), any());
		when(applicationContext.getBean(PublishService.class)).thenReturn(publishServiceSpy);

		var accepted = publishServiceSpy.publishPageAsTask(1L);

		assertEquals(3, accepted.getTotalSteps(), "the page itself plus the two gallery files");
		assertEquals(TaskStatusEnum.COMPLETED, taskStore.find(accepted.getTaskId())
														.orElseThrow()
														.getStatus());
	}

	@Test
	void publishGalleriesNowFailsTheRemainingFileStepsOfAGalleryThatCannotBePublished() throws Exception {
		var gallery = Gallery.builder()
							 .id(1L)
							 .aclId(101L)
							 .siteFiles(List.of(SiteFile.builder()
														.id(11L)
														.build(),
												SiteFile.builder()
														.id(12L)
														.build()))
							 .build();
		when(fileService.findGalleryById(1L)).thenReturn(gallery);
		when(fileService.findGalleryById(2L)).thenReturn(null);
		Mockito.doAnswer(invocation -> {
				   var progress = invocation.getArgument(1, TaskProgress.class);
				   progress.advance("Transferred a.jpg");
				   progress.advance("Transferred b.jpg");
				   return null;
			   })
			   .when(publishServiceSpy)
			   .publishGalleryAsSystem(org.mockito.ArgumentMatchers.eq(1L), any());
		Mockito.doThrow(new VempainEntityNotFoundException())
			   .when(publishServiceSpy)
			   .publishGalleryAsSystem(org.mockito.ArgumentMatchers.eq(2L), any());
		when(applicationContext.getBean(PublishService.class)).thenReturn(publishServiceSpy);
		var progress = taskStore.create("PUBLISH_SELECTED_GALLERIES", "Publish 2 selected galleries", 1L, 3);

		var result = publishServiceSpy.publishGalleriesNow(List.of(1L, 2L), 0, progress);

		assertEquals(Map.of("published", 1L, "skipped", 1L), result);
		assertEquals(3, progress.getCompletedSteps()
								.get(), "the missing gallery's single step is reported as failed");
		assertEquals(1, progress.getFailedSteps()
								.get());
	}

	// ---- publishPageAsSystem details, the gallery transfer and its site rows, the gallery task submission ----

	@Test
	void publishPageAsSystemRejectsAMissingPage() {
		when(pageService.findById(99L)).thenReturn(null);

		assertThrows(VempainEntityNotFoundException.class, () -> publishService.publishPageAsSystem(99L));
	}

	@Test
	void publishPageAsSystemSkipsMissingComponentsKeepsTheModifierUpdatesAnExistingSitePageAndPublishesItsGalleries() throws Exception {
		var page = Page.builder()
					   .id(1L)
					   .formId(2L)
					   .body("Body")
					   .pagePath("/page")
					   .title("Page")
					   .header("Header")
					   .creator(7L)
					   .modifier(8L)
					   .aclId(1L)
					   .build();
		var form = Form.builder()
					   .id(2L)
					   .layoutId(3L)
					   .build();
		var layout = Layout.builder()
						   .id(3L)
						   .structure("<!--comp_0--><!--comp_1--><main><!--page--></main>")
						   .build();
		when(pageService.findById(1L)).thenReturn(page);
		when(formService.findById(2L)).thenReturn(form);
		when(formService.findAllFormComponentsByFormId(2L)).thenReturn(List.of(FormComponent.builder()
																							.formId(2L)
																							.componentId(20L)
																							.sortOrder(0L)
																							.build(),
																			   FormComponent.builder()
																							.formId(2L)
																							.componentId(21L)
																							.sortOrder(1L)
																							.build()));
		when(componentService.findById(20L)).thenReturn(Component.builder()
																 .id(20L)
																 .compData("echo 'hi';")
																 .build());
		when(componentService.findById(21L)).thenThrow(new VempainComponentException("gone"));
		when(layoutService.findById(3L)).thenReturn(layout);
		when(userService.findUserResponseById(7L)).thenReturn(UserResponse.builder()
																		  .nick("creator")
																		  .build());
		when(userService.findUserResponseById(8L)).thenReturn(UserResponse.builder()
																		  .nick("modifier")
																		  .build());
		var existing = WebSitePage.builder()
								  .id(55L)
								  .aclId(66L)
								  .pageId(1L)
								  .build();
		when(webSitePageRepository.findByPageId(1L)).thenReturn(Optional.of(existing));
		when(webSitePageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(pageGalleryService.findPageGalleryByPageId(1L)).thenReturn(List.of(PageGallery.builder()
																						   .pageId(1L)
																						   .galleryId(5L)
																						   .build()));
		var progress = taskStore.create("PUBLISH_PAGE", "Publish page", 1L, 2);
		doNothing().when(publishServiceSpy)
				   .publishGalleryAsSystem(5L, progress);

		var sitePageId = publishServiceSpy.publishPageAsSystem(1L, progress);

		assertEquals(55L, sitePageId);
		assertEquals("modifier", existing.getModifier());
		assertEquals("<?php\necho 'hi';\n?><!--comp_1--><main>Body</main>", existing.getBody());
		assertEquals(66L, existing.getAclId(), "an existing site page keeps its ACL");
		assertEquals(1, progress.getCompletedSteps()
								.get());
		verify(publishServiceSpy).publishGalleryAsSystem(5L, progress);
		verify(webSiteResourceService, never()).getNextWebSiteAcl();
		verify(webSitePageRepository).resetCacheByPageId(10L);
	}

	private Gallery galleryWithFiles() {
		var location = GpsLocation.builder()
								  .id(900L)
								  .latitude(new BigDecimal("60.1"))
								  .latitudeRef('N')
								  .longitude(new BigDecimal("24.9"))
								  .longitudeRef('E')
								  .city("Helsinki")
								  .build();
		var image = SiteFile.builder()
							.id(11L)
							.aclId(111L)
							.fileType(FileTypeEnum.IMAGE)
							.filePath("2024/trip")
							.fileName("a.jpg")
							.location(location)
							.metadata("{}")
							.creator(1L)
							.build();
		var video = SiteFile.builder()
							.id(12L)
							.aclId(112L)
							.fileType(FileTypeEnum.VIDEO)
							.filePath("2024/trip")
							.fileName("b.mp4")
							.creator(1L)
							.build();
		return Gallery.builder()
					  .id(1L)
					  .aclId(101L)
					  .shortname("trip")
					  .description("Trip")
					  .siteFiles(List.of(image, video))
					  .build();
	}

	private List<GalleryFile> galleryFilesOf(Gallery gallery) {
		var files = new java.util.ArrayList<GalleryFile>();
		long order = 0;
		for (var siteFile : gallery.getSiteFiles()) {
			files.add(GalleryFile.builder()
								 .galleryId(gallery.getId())
								 .siteFileId(siteFile.getId())
								 .sortOrder(order++)
								 .build());
		}
		return files;
	}

	@Test
	void publishGalleryAsSystemRejectsAMissingGalleryAndReportsAnEmptyOneAsOneStep() throws Exception {
		when(fileService.findGalleryById(99L)).thenReturn(null);
		assertThrows(VempainEntityNotFoundException.class, () -> publishService.publishGalleryAsSystem(99L));

		var empty = Gallery.builder()
						   .id(2L)
						   .shortname("empty")
						   .siteFiles(List.of())
						   .build();
		when(fileService.findGalleryById(2L)).thenReturn(empty);
		when(galleryFileService.findGalleryFileByGalleryId(2L)).thenReturn(List.of());
		var progress = taskStore.create("PUBLISH_GALLERY", "Publish gallery", 1L, 1);

		publishService.publishGalleryAsSystem(2L, progress);

		assertEquals(1, progress.getCompletedSteps()
								.get());
		verify(jschClient, never()).connect(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any());
	}

	@Test
	void publishGalleryAsSystemTransfersTheFilesAndRewritesTheSiteRows() throws Exception {
		var gallery = galleryWithFiles();
		var image = gallery.getSiteFiles()
						   .getFirst();
		var video = gallery.getSiteFiles()
						   .get(1);
		var thumb = FileThumb.builder()
							 .id(5L)
							 .parentId(11L)
							 .parentType(FileTypeEnum.IMAGE)
							 .build();
		when(fileService.findGalleryById(1L)).thenReturn(gallery);
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(galleryFilesOf(gallery));
		when(fileService.findAllFileThumbsBySiteFileList(gallery.getSiteFiles())).thenReturn(List.of(thumb));
		ReflectionTestUtils.setField(publishService, "thumbSubDir", ".thumb");
		Mockito.doAnswer(invocation -> {
				   Consumer<SiteFile> callback = invocation.getArgument(2);
				   gallery.getSiteFiles()
						  .forEach(callback);
				   return null;
			   })
			   .when(jschClient)
			   .transferFilesToSite(eq(gallery.getSiteFiles()), eq(List.of(thumb)), any());
		when(webSiteResourceService.getNextWebSiteAcl()).thenReturn(1000L, 1001L, 1002L);
		when(webSiteGalleryRepository.save(any(WebSiteGallery.class))).thenAnswer(invocation -> {
			var saved = invocation.getArgument(0, WebSiteGallery.class);
			saved.setId(500L);
			return saved;
		});
		when(siteFileRepository.findById(11L)).thenReturn(Optional.of(image));
		when(siteFileRepository.findById(12L)).thenReturn(Optional.of(video));
		when(webGpsLocationRepository.findById(900L)).thenReturn(Optional.empty());
		when(webGpsLocationRepository.save(any(WebGpsLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));
		var savedFiles = new java.util.ArrayList<WebSiteFile>();
		when(webSiteFileRepository.save(any(WebSiteFile.class))).thenAnswer(invocation -> {
			var saved = invocation.getArgument(0, WebSiteFile.class);
			saved.setId(600L + savedFiles.size());
			savedFiles.add(saved);
			return saved;
		});
		var subjects = List.of(Subject.builder()
									  .id(1L)
									  .subjectName("sea")
									  .build());
		when(subjectService.getSubjectsByFileId(11L)).thenReturn(subjects);
		when(subjectService.getSubjectsByFileId(12L)).thenReturn(List.of());
		var siteSubjects = List.of(WebSiteSubject.builder()
												 .id(1L)
												 .build());
		when(webSiteSubjectService.saveAllFromAdminSubject(subjects)).thenReturn(siteSubjects);
		when(webSiteSubjectService.saveAllFromAdminSubject(List.of())).thenReturn(List.of());
		var progress = taskStore.create("PUBLISH_GALLERY", "Publish gallery", 1L, 2);

		publishService.publishGalleryAsSystem(1L, progress);

		assertEquals(image, thumb.getSiteFile());
		assertEquals(2, progress.getCompletedSteps()
								.get());
		verify(jschClient).close();
		verify(webSiteGalleryRepository).deleteByGalleryId(1L);
		verify(webSiteFileRepository).deleteByFileId(11L);
		verify(webSiteFileRepository).deleteByFileId(12L);
		assertEquals(2, savedFiles.size());
		var publishedImage = savedFiles.getFirst();
		assertEquals("image/2024/trip/a.jpg", publishedImage.getFilePath());
		assertEquals("image/2024/trip/.thumb/a.jpg", publishedImage.getThumbnailPath());
		assertNotNull(publishedImage.getLocation());
		assertEquals("Helsinki", publishedImage.getLocation()
											   .getCity());
		var publishedVideo = savedFiles.get(1);
		assertNull(publishedVideo.getThumbnailPath());
		assertNull(publishedVideo.getLocation());
		verify(webSiteGalleryRepository).saveGalleryFile(500L, 600L, 0L);
		verify(webSiteGalleryRepository).saveGalleryFile(500L, 601L, 1L);
		verify(webSiteSubjectService).saveSiteFileSubject(600L, siteSubjects);
		verify(webSiteSubjectService).saveSiteFileSubject(601L, List.of());
	}

	@Test
	void publishGalleryAsSystemFailsWhenAThumbHasNoSiteFileInTheGallery() throws Exception {
		var gallery = galleryWithFiles();
		var orphanThumb = FileThumb.builder()
								   .id(6L)
								   .parentId(999L)
								   .build();
		when(fileService.findGalleryById(1L)).thenReturn(gallery);
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(galleryFilesOf(gallery));
		when(fileService.findAllFileThumbsBySiteFileList(gallery.getSiteFiles())).thenReturn(List.of(orphanThumb));

		assertThrows(VempainEntityNotFoundException.class, () -> publishService.publishGalleryAsSystem(1L));
		verify(jschClient, never()).connect(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any());
	}

	@Test
	void sshAndSftpFailuresDuringTheTransferAreInternalErrorsAndTheConnectionIsAlwaysClosed() throws Exception {
		var gallery = galleryWithFiles();
		when(fileService.findGalleryById(1L)).thenReturn(gallery);
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(galleryFilesOf(gallery));
		when(fileService.findAllFileThumbsBySiteFileList(gallery.getSiteFiles())).thenReturn(List.of());

		doThrow(new JSchException("refused")).when(jschClient)
											 .connect(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any());
		var ssh = assertThrows(ResponseStatusException.class, () -> publishService.publishGalleryAsSystem(1L));
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ssh.getStatusCode());

		doNothing().when(jschClient)
				   .connect(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any());
		doThrow(new SftpException(4, "failure")).when(jschClient)
												.transferFilesToSite(any(), any(), any());
		var sftp = assertThrows(ResponseStatusException.class, () -> publishService.publishGalleryAsSystem(1L));
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, sftp.getStatusCode());

		verify(jschClient, times(2)).close();
		verify(webSiteGalleryRepository, never()).deleteByGalleryId(anyLong());
	}

	@Test
	void publishSelectedGalleriesRejectsAnEmptyListAndSkipsInvalidAndVanishedGalleries() throws Exception {
		assertThrows(ResponseStatusException.class, () -> publishService.publishSelectedGalleries(null));
		assertThrows(ResponseStatusException.class, () -> publishService.publishSelectedGalleries(List.of()));

		var gallery = Gallery.builder()
							 .id(1L)
							 .aclId(101L)
							 .build();
		when(fileService.findGalleryById(1L)).thenReturn(gallery);
		when(accessService.hasModifyPermission(101L)).thenReturn(true);
		doThrow(new VempainEntityNotFoundException()).when(publishServiceSpy)
													 .publishGalleryAsSystem(1L);
		var ids = new java.util.ArrayList<Long>();
		ids.add(null);
		ids.add(0L);
		ids.add(1L);

		var response = publishServiceSpy.publishSelectedGalleries(ids);

		assertEquals(PublishResultEnum.FAIL, response.getResult());
		assertEquals("Published 0 galleries, skipped 3", response.getMessage());
	}

	@Test
	void publishAllGalleriesAsTaskSkipsGalleriesTheUserMayNotPublishAtSubmitTime() throws Exception {
		var allowed = Gallery.builder()
							 .id(1L)
							 .aclId(101L)
							 .siteFiles(List.of(SiteFile.builder()
														.id(11L)
														.aclId(111L)
														.build()))
							 .build();
		var denied = Gallery.builder()
							.id(2L)
							.aclId(102L)
							.build();
		when(fileService.findAllGalleries()).thenReturn(List.of(allowed, denied));
		when(fileService.findGalleryById(1L)).thenReturn(allowed);
		when(fileService.findGalleryById(2L)).thenReturn(denied);
		when(accessService.hasModifyPermission(101L)).thenReturn(true);
		when(accessService.hasModifyPermission(102L)).thenReturn(false);
		when(accessService.hasReadPermission(111L)).thenReturn(true);
		Mockito.doAnswer(invocation -> {
				   invocation.getArgument(1, TaskProgress.class)
							 .advance("Transferred a.jpg");
				   return null;
			   })
			   .when(publishServiceSpy)
			   .publishGalleryAsSystem(eq(1L), any());
		when(applicationContext.getBean(PublishService.class)).thenReturn(publishServiceSpy);

		var accepted = publishServiceSpy.publishAllGalleriesAsTask();

		assertEquals(1, accepted.getTotalSteps());
		var task = taskStore.find(accepted.getTaskId())
							.orElseThrow();
		assertEquals(TaskStatusEnum.COMPLETED, task.getStatus());
		assertEquals(Map.of("published", 1L, "skipped", 1L), task.getResult());
		verify(publishServiceSpy, never()).publishGalleryAsSystem(eq(2L), any());
	}
}
