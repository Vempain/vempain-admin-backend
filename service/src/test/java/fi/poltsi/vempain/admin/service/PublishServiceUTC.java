package fi.poltsi.vempain.admin.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

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
import fi.poltsi.vempain.site.service.SiteDatabaseRetry;
import fi.poltsi.vempain.site.service.WebSiteResourceService;
import fi.poltsi.vempain.site.service.WebSiteSubjectService;
import fi.poltsi.vempain.tools.JschClient;

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
    private SiteFileRepository siteFileRepository;
    @Mock
    private WebSitePageRepository webSitePageRepository;
    @Mock
    private WebSiteGalleryRepository webSiteGalleryRepository;
    @Mock
    private WebSiteFileRepository webSiteFileRepository;
    @Mock
    private WebGpsLocationRepository webGpsLocationRepository;
    @Mock
    private PageService pageService;
    @Mock
    private FormService formService;
    @Mock
    private ComponentService componentService;
    @Mock
    private FileService fileService;
    @Mock
    private LayoutService layoutService;
    @Mock
    private UserService userService;
    @Mock
    private SubjectService subjectService;
    @Mock
    private GalleryFileService galleryFileService;
    @Mock
    private WebSiteSubjectService webSiteSubjectService;
    @Mock
    private PageGalleryService pageGalleryService;
    @Mock
    private JschClient jschClient;
    @Mock
    private WebSiteResourceService webSiteResourceService;
    @Mock
    private AccessService accessService;
    @Mock
    private ApplicationContext applicationContext;

    // A synchronous task runner: submitted work runs on the calling thread, so the task state can be asserted right away
    private final TaskProgressStore taskStore = new TaskProgressStore();
    @Spy
    private TaskRunner taskRunner = new TaskRunner(taskStore, Runnable::run);

    @Mock
    private TagBlacklistService tagBlacklistService;
    // Site writes run through the retry helper on a stubbed site transaction manager, without pauses
    @Spy
    private SiteDatabaseRetry siteDatabaseRetry = new SiteDatabaseRetry(Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class), null, 3,
            0, millis -> {});
    @InjectMocks
    private PublishService publishService;

    private PublishService publishServiceSpy;

    @BeforeEach
    void setupSpy() {
        publishServiceSpy = Mockito.spy(publishService);
        Mockito.lenient()
            .when(tagBlacklistService.snapshot())
            .thenReturn(TagBlacklist.EMPTY);
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
            .build(),
                SiteFile.builder()
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
            .pageStyle(".vps-page-canvas { max-width: 60rem; }")
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
        when(webSitePageRepository.save(any())).thenAnswer(invocation -> {
            var sitePage = invocation.getArgument(0, WebSitePage.class);
            sitePage.setId(300L);
            return sitePage;
        });
        // The page carries two tags, one of them blacklisted: only the other one is published, attributed to the page's creator
        var travel = Subject.builder()
            .id(1L)
            .subjectName("travel")
            .build();
        when(subjectService.getSubjectsByPageId(1L)).thenReturn(List.of(travel, Subject.builder()
            .id(2L)
            .subjectName("private")
            .build()));
        when(tagBlacklistService.snapshot()).thenReturn(TagBlacklist.of(List.of("private")));
        var siteTravel = WebSiteSubject.builder()
            .id(50L)
            .build();
        when(webSiteSubjectService.saveAllFromAdminSubject(List.of(travel), "creator")).thenReturn(List.of(siteTravel));

        publishService.publishPageAsSystem(1L);

        verify(webSitePageRepository).save(argThat(sitePage -> "<main>Page body</main>".equals(sitePage.getBody())));
        // the custom CSS of the page is published with it, as the website's web_site_page.page_style
        verify(webSitePageRepository).save(argThat(sitePage -> ".vps-page-canvas { max-width: 60rem; }".equals(sitePage.getPageStyle())));
        verify(webSiteSubjectService).replaceSitePageSubjects(300L, List.of(siteTravel));
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

        verify(taskRunner, never()).submitDurable(any(), any(), anyLong(), any(), any(), any());
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
    void publishSelectedFilesAsTaskSkipsInvalidMissingAndUnauthorizedFilesAtSubmitTime() throws Exception {
        var allowed = SiteFile.builder()
            .id(5L)
            .aclId(105L)
            .build();
        var denied = SiteFile.builder()
            .id(6L)
            .aclId(106L)
            .build();
        when(fileService.findSiteFileById(5L)).thenReturn(Optional.of(allowed));
        when(fileService.findSiteFileById(6L)).thenReturn(Optional.of(denied));
        when(fileService.findSiteFileById(7L)).thenReturn(Optional.empty());
        when(accessService.hasModifyPermission(105L)).thenReturn(true);
        when(accessService.hasModifyPermission(106L)).thenReturn(false);
        Mockito.doAnswer(invocation -> {
            invocation.getArgument(1, TaskProgress.class)
                .advance("Transferred a.jpg of file a.jpg");
            return null;
        })
            .when(publishServiceSpy)
            .publishFileAsSystem(eq(5L), any());
        when(applicationContext.getBean(PublishService.class)).thenReturn(publishServiceSpy);

        var accepted = publishServiceSpy.publishSelectedFilesAsTask(List.of(5L, 6L, 7L, 0L));

        assertEquals("PUBLISH_SELECTED_FILES", accepted.getType());
        assertEquals(1, accepted.getTotalSteps());
        var task = taskStore.find(accepted.getTaskId())
            .orElseThrow();
        assertEquals(TaskStatusEnum.COMPLETED, task.getStatus());
        assertEquals(Map.of("published", 1L, "skipped", 3L), task.getResult());
        verify(publishServiceSpy).publishFileAsSystem(eq(5L), any());
        verify(publishServiceSpy, never()).publishFileAsSystem(eq(6L), any());
        verify(taskRunner).submitDurable(eq("PUBLISH_SELECTED_FILES"), eq("Publish 1 selected files"), eq(1L),
                eq(Map.of("file_ids", List.of(5L), "skipped", 3)), eq(publishService.siteTransferLane()), any());
    }

    @Test
    void publishSelectedFilesAsTaskRejectsAnEmptyList() {
        assertThrows(ResponseStatusException.class, () -> publishService.publishSelectedFilesAsTask(List.of()));
        assertEquals(0, taskStore.size());
    }

    @Test
    void publishFilesNowFailsTheStepOfAMissingFileWithoutFailingTheTask() throws Exception {
        Mockito.doAnswer(invocation -> {
            invocation.getArgument(1, TaskProgress.class)
                .advance("Transferred a.jpg of file a.jpg");
            return null;
        })
            .when(publishServiceSpy)
            .publishFileAsSystem(eq(1L), any());
        doThrow(new VempainEntityNotFoundException()).when(publishServiceSpy)
            .publishFileAsSystem(eq(2L), any());
        when(applicationContext.getBean(PublishService.class)).thenReturn(publishServiceSpy);
        var progress = taskStore.create("PUBLISH_SELECTED_FILES", "Publish 2 selected files", 1L, 2);

        var result = publishServiceSpy.publishFilesNow(List.of(1L, 2L), 1, progress);

        assertEquals(Map.of("published", 1L, "skipped", 2L), result);
        assertEquals(2, progress.getCompletedSteps()
            .get());
        assertEquals(1, progress.getFailedSteps()
            .get());
    }

    @Test
    void publishFileAsSystemTransfersTheFileWithItsThumbnailAndWritesItsSiteRowWithoutAGallery() throws Exception {
        var image = galleryWithFiles().getSiteFiles()
            .getFirst();
        var thumb = FileThumb.builder()
            .id(5L)
            .parentId(11L)
            .parentType(FileTypeEnum.IMAGE)
            .build();
        when(fileService.findSiteFileById(11L)).thenReturn(Optional.of(image));
        when(fileService.findAllFileThumbsBySiteFileList(List.of(image))).thenReturn(List.of(thumb));
        ReflectionTestUtils.setField(publishService, "thumbSubDir", ".thumb");
        var connection = org.mockito.Mockito.mock(JschClient.Connection.class);
        when(jschClient.connect(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any())).thenReturn(connection);
        Mockito.doAnswer(invocation -> {
            Consumer<SiteFile> callback = invocation.getArgument(2);
            callback.accept(image);
            return null;
        })
            .when(connection)
            .transferFilesToSite(eq(List.of(image)), eq(List.of(thumb)), any());
        when(userService.findUserResponseById(anyLong())).thenReturn(UserResponse.builder()
            .nick("alice")
            .build());
        when(webGpsLocationRepository.findById(900L)).thenReturn(Optional.empty());
        when(webGpsLocationRepository.save(any(WebGpsLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(webSiteResourceService.getNextWebSiteAcl()).thenReturn(1000L);
        when(webSiteFileRepository.save(any(WebSiteFile.class))).thenAnswer(invocation -> {
            var saved = invocation.getArgument(0, WebSiteFile.class);
            saved.setId(600L);
            return saved;
        });
        when(subjectService.getSubjectsByFileId(11L)).thenReturn(List.of());
        when(webSiteSubjectService.saveAllFromAdminSubject(List.of(), "alice")).thenReturn(List.of());
        var progress = taskStore.create("PUBLISH_SELECTED_FILES", "Publish 1 selected files", 1L, 1);

        publishService.publishFileAsSystem(11L, progress);

        assertEquals(image, thumb.getSiteFile());
        assertEquals(1, progress.getCompletedSteps()
            .get());
        verify(connection).close();
        var savedFile = org.mockito.ArgumentCaptor.forClass(WebSiteFile.class);
        verify(webSiteFileRepository).save(savedFile.capture());
        assertEquals("image/2024/trip/a.jpg", savedFile.getValue()
            .getFilePath());
        assertEquals("image/2024/trip/.thumb/a.jpg", savedFile.getValue()
            .getThumbnailPath());
        assertEquals(1000L, savedFile.getValue()
            .getAclId());
        verify(webSiteSubjectService).replaceSiteFileSubjects(600L, List.of());
        // No gallery is involved: no site gallery is written and the file is linked to none
        verify(webSiteGalleryRepository, never()).save(any(WebSiteGallery.class));
        verify(webSiteGalleryRepository, never()).saveGalleryFile(anyLong(), anyLong(), anyLong());
        assertNotNull(image.getPublished());
        verify(siteFileRepository).save(image);
    }

    @Test
    void publishFileAsSystemRejectsAMissingFile() throws Exception {
        when(fileService.findSiteFileById(99L)).thenReturn(Optional.empty());
        assertThrows(VempainEntityNotFoundException.class, () -> publishService.publishFileAsSystem(99L, null));
        verify(jschClient, never()).connect(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any());
    }

    @Test
    void removeFileFromSiteDeletesTheSiteRowsOfTheFileAndTheirGalleryLinks() throws Exception {
        var image = galleryWithFiles().getSiteFiles()
            .getFirst();
        var siteImage = WebSiteFile.builder()
            .id(600L)
            .fileId(11L)
            .filePath("image/2024/trip/a.jpg")
            .build();
        when(webSiteFileRepository.findByFileIdOrFilePathOrThumbnailPathOrderByIdAsc(11L, "image/2024/trip/a.jpg", null)).thenReturn(List.of(siteImage));

        publishService.removeFileFromSite(image);

        verify(webSiteGalleryRepository).deleteGalleryFilesByFileId(600L);
        verify(webSiteFileRepository).deleteAll(List.of(siteImage));
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
            .created(Instant.parse("2026-01-01T10:00:00Z"))
            .modifier(2L)
            .modified(Instant.parse("2026-01-02T10:00:00Z"))
            .build();
        var video = SiteFile.builder()
            .id(12L)
            .aclId(112L)
            .fileType(FileTypeEnum.VIDEO)
            .filePath("2024/trip")
            .fileName("b.mp4")
            .creator(1L)
            .created(Instant.parse("2026-01-01T10:00:00Z"))
            .build();
        return Gallery.builder()
            .id(1L)
            .aclId(101L)
            .shortname("trip")
            .description("Trip")
            .creator(1L)
            .created(Instant.parse("2026-01-01T09:00:00Z"))
            .modifier(2L)
            .modified(Instant.parse("2026-01-03T09:00:00Z"))
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
        // Nothing was released, so the gallery keeps its "never published" state
        assertNull(empty.getPublished());
        verify(fileService, never()).saveGallery(any());
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
        var connection = org.mockito.Mockito.mock(JschClient.Connection.class);
        when(jschClient.connect(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any())).thenReturn(connection);
        Mockito.doAnswer(invocation -> {
            Consumer<SiteFile> callback = invocation.getArgument(2);
            gallery.getSiteFiles()
                .forEach(callback);
            return null;
        })
            .when(connection)
            .transferFilesToSite(eq(gallery.getSiteFiles()), eq(List.of(thumb)), any());
        when(webSiteResourceService.getNextWebSiteAcl()).thenReturn(1000L, 1001L, 1002L);
        var savedGalleries = new java.util.ArrayList<WebSiteGallery>();
        when(webSiteGalleryRepository.save(any(WebSiteGallery.class))).thenAnswer(invocation -> {
            var saved = invocation.getArgument(0, WebSiteGallery.class);
            saved.setId(500L);
            savedGalleries.add(saved);
            return saved;
        });
        when(siteFileRepository.findById(11L)).thenReturn(Optional.of(image));
        when(siteFileRepository.findById(12L)).thenReturn(Optional.of(video));
        when(userService.findUserResponseById(1L)).thenReturn(UserResponse.builder()
            .nick("alice")
            .build());
        when(userService.findUserResponseById(2L)).thenReturn(UserResponse.builder()
            .nick("bob")
            .build());
        when(webGpsLocationRepository.findById(900L)).thenReturn(Optional.empty());
        when(webGpsLocationRepository.save(any(WebGpsLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var savedFiles = new java.util.ArrayList<WebSiteFile>();
        when(webSiteFileRepository.save(any(WebSiteFile.class))).thenAnswer(invocation -> {
            var saved = invocation.getArgument(0, WebSiteFile.class);
            saved.setId(600L + savedFiles.size());
            savedFiles.add(saved);
            return saved;
        });
        var sea = Subject.builder()
            .id(1L)
            .subjectName("sea")
            .build();
        var subjects = List.of(sea);
        // The image also carries a blacklisted tag, in its subjects and in the keyword lists of its metadata: neither reaches the site
        when(tagBlacklistService.snapshot()).thenReturn(TagBlacklist.of(List.of("Private")));
        image.setMetadata("[{\"Keywords\":[\"sea\",\"private\"],\"XMP-dc\":{\"Subject\":[\"private\"]},\"Make\":\"Canon\"}]");
        when(subjectService.getSubjectsByFileId(11L)).thenReturn(List.of(sea, Subject.builder()
            .id(2L)
            .subjectName("private")
            .build()));
        when(subjectService.getSubjectsByFileId(12L)).thenReturn(List.of());
        var siteSubjects = List.of(WebSiteSubject.builder()
            .id(1L)
            .build());
        // The subjects of the image are attributed to its modifier, those of the video (never modified) to its creator
        when(webSiteSubjectService.saveAllFromAdminSubject(subjects, "bob")).thenReturn(siteSubjects);
        when(webSiteSubjectService.saveAllFromAdminSubject(List.of(), "alice")).thenReturn(List.of());
        var progress = taskStore.create("PUBLISH_GALLERY", "Publish gallery", 1L, 2);

        publishService.publishGalleryAsSystem(1L, progress);

        assertEquals(image, thumb.getSiteFile());
        assertEquals(2, progress.getCompletedSteps()
            .get());
        // The transfer ran over a connection of its own, closed when it was done
        verify(connection).close();
        // Nothing of the gallery was on the site yet: every row is new, with a site ACL of its own
        verify(webSiteGalleryRepository).findAllByGalleryIdOrderByIdAsc(1L);
        verify(webSiteGalleryRepository).deleteGalleryFiles(500L);
        verify(webSiteFileRepository).findByFileIdOrFilePathOrThumbnailPathOrderByIdAsc(11L, "image/2024/trip/a.jpg", "image/2024/trip/.thumb/a.jpg");
        verify(webSiteFileRepository).findByFileIdOrFilePathOrThumbnailPathOrderByIdAsc(12L, "video/2024/trip/b.mp4", null);
        verify(webSiteGalleryRepository, never()).deleteAll(any());
        verify(webSiteFileRepository, never()).deleteAll(any());
        assertEquals(1000L, savedGalleries.getFirst()
            .getAclId());
        assertEquals(2, savedFiles.size());
        var publishedImage = savedFiles.getFirst();
        assertEquals("image/2024/trip/a.jpg", publishedImage.getFilePath());
        var publishedMetadata = new org.json.JSONArray(publishedImage.getMetadata()).getJSONObject(0);
        assertEquals(List.of("sea"), publishedMetadata.getJSONArray("Keywords")
            .toList());
        assertTrue(publishedMetadata.getJSONObject("XMP-dc")
            .isEmpty());
        assertEquals("Canon", publishedMetadata.getString("Make"));
        assertEquals("image/2024/trip/.thumb/a.jpg", publishedImage.getThumbnailPath());
        assertNotNull(publishedImage.getLocation());
        assertEquals("Helsinki", publishedImage.getLocation()
            .getCity());
        // The site gallery and file carry the admin audit trail with the users named by nick
        assertEquals(1, savedGalleries.size());
        assertEquals("alice", savedGalleries.getFirst()
            .getCreator());
        assertEquals("bob", savedGalleries.getFirst()
            .getModifier());
        assertEquals(Instant.parse("2026-01-01T09:00:00Z"), savedGalleries.getFirst()
            .getCreated());
        assertEquals("alice", publishedImage.getCreator());
        assertEquals(Instant.parse("2026-01-01T10:00:00Z"), publishedImage.getCreated());
        assertEquals("bob", publishedImage.getModifier());
        assertEquals(Instant.parse("2026-01-02T10:00:00Z"), publishedImage.getModified());
        // A location new to the site is created by the file's actor at publish time
        assertEquals("bob", publishedImage.getLocation()
            .getCreator());
        assertNotNull(publishedImage.getLocation()
            .getCreated());
        assertNull(publishedImage.getLocation()
            .getModifier());
        var publishedVideo = savedFiles.get(1);
        assertNull(publishedVideo.getThumbnailPath());
        assertNull(publishedVideo.getLocation());
        assertEquals("alice", publishedVideo.getCreator());
        assertNull(publishedVideo.getModifier());
        assertNull(publishedVideo.getModified());
        verify(webSiteGalleryRepository).saveGalleryFile(500L, 600L, 0L);
        verify(webSiteGalleryRepository).saveGalleryFile(500L, 601L, 1L);
        assertEquals(1001L, publishedImage.getAclId());
        verify(webSiteSubjectService).replaceSiteFileSubjects(600L, siteSubjects);
        // The release time is recorded on the admin side for the gallery and every file in it
        assertNotNull(gallery.getPublished());
        assertEquals(gallery.getPublished(), image.getPublished());
        assertEquals(gallery.getPublished(), video.getPublished());
        verify(siteFileRepository).save(image);
        verify(siteFileRepository).save(video);
        verify(fileService).saveGallery(gallery);
        verify(webSiteSubjectService).replaceSiteFileSubjects(601L, List.of());
    }

    @Test
    void republishingAGalleryUpdatesTheSiteRowsInPlaceAndRemovesStaleDuplicates() throws Exception {
        var gallery = galleryWithFiles();
        var image = gallery.getSiteFiles()
            .getFirst();
        var video = gallery.getSiteFiles()
            .get(1);
        when(fileService.findGalleryById(1L)).thenReturn(gallery);
        when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(galleryFilesOf(gallery));
        when(fileService.findAllFileThumbsBySiteFileList(gallery.getSiteFiles())).thenReturn(List.of());
        ReflectionTestUtils.setField(publishService, "thumbSubDir", ".thumb");
        var connection = org.mockito.Mockito.mock(JschClient.Connection.class);
        when(jschClient.connect(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any())).thenReturn(connection);
        when(siteFileRepository.findById(11L)).thenReturn(Optional.of(image));
        when(siteFileRepository.findById(12L)).thenReturn(Optional.of(video));
        when(userService.findUserResponseById(anyLong())).thenReturn(UserResponse.builder()
            .nick("alice")
            .build());
        when(webGpsLocationRepository.findById(900L)).thenReturn(Optional.empty());
        when(webGpsLocationRepository.save(any(WebGpsLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subjectService.getSubjectsByFileId(anyLong())).thenReturn(List.of());
        when(webSiteSubjectService.saveAllFromAdminSubject(any(), any())).thenReturn(List.of());
        // The gallery was published before, twice by mistake: the first site row is kept, the stale one goes
        var siteGallery = WebSiteGallery.builder()
            .id(500L)
            .galleryId(1L)
            .aclId(70L)
            .shortname("old name")
            .build();
        var staleGallery = WebSiteGallery.builder()
            .id(501L)
            .galleryId(1L)
            .aclId(72L)
            .build();
        when(webSiteGalleryRepository.findAllByGalleryIdOrderByIdAsc(1L)).thenReturn(List.of(siteGallery, staleGallery));
        when(webSiteGalleryRepository.save(any(WebSiteGallery.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // The image is on the site already (its thumbnail path is unique there); the video is new to the site
        var siteImage = WebSiteFile.builder()
            .id(600L)
            .fileId(11L)
            .aclId(71L)
            .filePath("image/2024/trip/a.jpg")
            .thumbnailPath("image/2024/trip/.thumb/a.jpg")
            .build();
        when(webSiteFileRepository.findByFileIdOrFilePathOrThumbnailPathOrderByIdAsc(11L, "image/2024/trip/a.jpg", "image/2024/trip/.thumb/a.jpg"))
            .thenReturn(List.of(siteImage));
        when(webSiteFileRepository.findByFileIdOrFilePathOrThumbnailPathOrderByIdAsc(12L, "video/2024/trip/b.mp4", null)).thenReturn(List.of());
        when(webSiteResourceService.getNextWebSiteAcl()).thenReturn(1000L);
        var savedFiles = new java.util.ArrayList<WebSiteFile>();
        when(webSiteFileRepository.save(any(WebSiteFile.class))).thenAnswer(invocation -> {
            var saved = invocation.getArgument(0, WebSiteFile.class);
            if (saved.getId() == 0) {
                saved.setId(601L);
            }
            savedFiles.add(saved);
            return saved;
        });

        publishService.publishGalleryAsSystem(1L);

        // The gallery keeps its site id and ACL, gets the current name, and its file relations are written again
        var savedGallery = org.mockito.ArgumentCaptor.forClass(WebSiteGallery.class);
        verify(webSiteGalleryRepository).save(savedGallery.capture());
        assertEquals(500L, savedGallery.getValue()
            .getId());
        assertEquals(70L, savedGallery.getValue()
            .getAclId());
        assertEquals("trip", savedGallery.getValue()
            .getShortname());
        verify(webSiteGalleryRepository).deleteAll(List.of(staleGallery));
        verify(webSiteGalleryRepository).flush();
        verify(webSiteGalleryRepository).deleteGalleryFiles(500L);
        // The image row is updated in place, the video row is created with the only new site ACL
        assertEquals(2, savedFiles.size());
        var publishedImage = savedFiles.getFirst();
        assertEquals(600L, publishedImage.getId());
        assertEquals(71L, publishedImage.getAclId());
        assertEquals("image/2024/trip/.thumb/a.jpg", publishedImage.getThumbnailPath());
        assertEquals("Helsinki", publishedImage.getLocation()
            .getCity());
        var publishedVideo = savedFiles.get(1);
        assertEquals(601L, publishedVideo.getId());
        assertEquals(1000L, publishedVideo.getAclId());
        verify(webSiteResourceService, times(1)).getNextWebSiteAcl();
        verify(webSiteFileRepository, never()).deleteAll(any());
        verify(webSiteGalleryRepository).saveGalleryFile(500L, 600L, 0L);
        verify(webSiteGalleryRepository).saveGalleryFile(500L, 601L, 1L);
        verify(webSiteSubjectService).replaceSiteFileSubjects(600L, List.of());
        verify(webSiteSubjectService).replaceSiteFileSubjects(601L, List.of());
        assertNotNull(gallery.getPublished());
        assertEquals(gallery.getPublished(), image.getPublished());
        assertEquals(gallery.getPublished(), video.getPublished());
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

        var connection = org.mockito.Mockito.mock(JschClient.Connection.class);
        Mockito.doReturn(connection)
            .when(jschClient)
            .connect(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any());
        doThrow(new SftpException(4, "failure")).when(connection)
            .transferFilesToSite(any(), any(), any());
        var sftp = assertThrows(ResponseStatusException.class, () -> publishService.publishGalleryAsSystem(1L));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, sftp.getStatusCode());

        // The refused session never produced a connection; the one that failed while transferring is closed anyway
        verify(connection, times(1)).close();
        verify(webSiteGalleryRepository, never()).save(any(WebSiteGallery.class));
    }

    @Test
    void aSiteDatabaseConnectionLostWhileWritingTheGalleryIsRetriedAfterTheTransfer() throws Exception {
        var gallery = galleryWithFiles();
        when(fileService.findGalleryById(1L)).thenReturn(gallery);
        when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(galleryFilesOf(gallery));
        when(fileService.findAllFileThumbsBySiteFileList(gallery.getSiteFiles())).thenReturn(List.of());
        var connection = org.mockito.Mockito.mock(JschClient.Connection.class);
        when(jschClient.connect(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any())).thenReturn(connection);
        when(tagBlacklistService.snapshot()).thenReturn(TagBlacklist.of(List.of()));
        when(userService.findUserResponseById(anyLong())).thenReturn(UserResponse.builder()
            .nick("alice")
            .build());
        // The tunnel drops on the first site write; the pooled connection is dead, the second attempt gets a fresh one
        when(webSiteGalleryRepository.findAllByGalleryIdOrderByIdAsc(1L))
            .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("An I/O error occurred while sending to the backend",
                    new java.sql.SQLException("I/O error", "08006")))
            .thenReturn(List.of());
        when(webSiteResourceService.getNextWebSiteAcl()).thenReturn(1000L, 1001L, 1002L, 1003L);
        when(webSiteGalleryRepository.save(any(WebSiteGallery.class))).thenAnswer(invocation -> {
            var saved = invocation.getArgument(0, WebSiteGallery.class);
            saved.setId(500L);
            return saved;
        });
        when(siteFileRepository.findById(anyLong())).thenAnswer(invocation -> gallery.getSiteFiles()
            .stream()
            .filter(file -> file.getId() == invocation.getArgument(0, Long.class))
            .findFirst());
        when(webSiteFileRepository.save(any(WebSiteFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subjectService.getSubjectsByFileId(anyLong())).thenReturn(List.of());
        when(webSiteSubjectService.saveAllFromAdminSubject(any(), any())).thenReturn(List.of());

        publishService.publishGalleryAsSystem(1L);

        // The files were transferred once, the site rows were written on the second attempt, and the gallery is marked published
        verify(connection, times(1)).transferFilesToSite(any(), any(), any());
        verify(webSiteGalleryRepository, times(2)).findAllByGalleryIdOrderByIdAsc(1L);
        verify(webSiteGalleryRepository, times(1)).save(any(WebSiteGallery.class));
        verify(fileService).saveGallery(gallery);
        assertNotNull(gallery.getPublished());
    }

    @Test
    void publishTasksShareTheSiteSftpLaneWithTheConfiguredConcurrency() {
        // Unset (0 in this test) falls back to one transfer at a time
        assertEquals(fi.poltsi.vempain.common.task.TaskLane.of("site-sftp", 1), publishService.siteTransferLane());

        ReflectionTestUtils.setField(publishService, "siteSftpConcurrency", 3);

        assertEquals(fi.poltsi.vempain.common.task.TaskLane.of("site-sftp", 3), publishService.siteTransferLane());
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
