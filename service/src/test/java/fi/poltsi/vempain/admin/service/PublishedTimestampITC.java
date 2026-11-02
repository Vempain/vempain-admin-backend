package fi.poltsi.vempain.admin.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.service.file.GalleryFileService;
import fi.poltsi.vempain.common.task.TaskProgressStore;
import fi.poltsi.vempain.tools.JschClient;

import static fi.poltsi.vempain.admin.api.Constants.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The release time of a gallery publish must reach the admin database for the gallery and for every file of the gallery. The test runs outside the usual test
 * transaction, so that it sees what was really committed, and replaces the SFTP transfer by a stub.
 */
class PublishedTimestampITC extends AbstractITCTest {

    @MockitoBean
    private JschClient jschClient;
    @Autowired
    private GalleryFileService galleryFileService;
    @Autowired
    @Qualifier("adminTransactionManager")
    private PlatformTransactionManager adminTransactionManager;
    @Autowired
    private TaskProgressStore taskProgressStore;
    @Autowired
    private PageGalleryService pageGalleryService;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void publishingAGalleryRecordsTheReleaseTimeOfTheGalleryAndOfEveryFile() throws Exception {
        var connection = mock(JschClient.Connection.class);
        when(jschClient.connect(any(), anyInt(), any(), any(), any())).thenReturn(connection);
        Long galleryId = new TransactionTemplate(adminTransactionManager).execute(status -> testITCTools.generateGalleryFromDirectory(ADMIN_ID));
        assertNotNull(galleryId);
        var before = Instant.now()
            .truncatedTo(ChronoUnit.SECONDS);

        publishService.publishGalleryAsSystem(galleryId);

        var gallery = fileService.findGalleryById(galleryId);
        assertNotNull(gallery.getPublished(), "the gallery records its release");
        var galleryFiles = galleryFileService.findGalleryFileByGalleryId(galleryId);
        assertFalse(galleryFiles.isEmpty());
        assertFilesReleasedSince(galleryFiles, before);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void theGalleryPublishTaskRecordsTheReleaseTimeOfEveryFile() throws Exception {
        var connection = mock(JschClient.Connection.class);
        when(jschClient.connect(any(), anyInt(), any(), any(), any())).thenReturn(connection);
        Long galleryId = new TransactionTemplate(adminTransactionManager).execute(status -> testITCTools.generateGalleryFromDirectory(ADMIN_ID));
        assertNotNull(galleryId);
        var before = Instant.now()
            .truncatedTo(ChronoUnit.SECONDS);

        // The way the gallery list publishes: a background task on a worker thread, with progress rows committed in transactions of their own
        var accepted = publishService.publishGalleryAsTask(galleryId);
        awaitTaskCompletion(accepted.getTaskId());

        assertNotNull(fileService.findGalleryById(galleryId)
            .getPublished(), "the gallery records its release");
        assertFilesReleasedSince(galleryFileService.findGalleryFileByGalleryId(galleryId), before);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void thePagePublishTaskRecordsTheReleaseTimeOfTheFilesOfItsGallery() throws Exception {
        var connection = mock(JschClient.Connection.class);
        when(jschClient.connect(any(), anyInt(), any(), any(), any())).thenReturn(connection);
        var ids = new TransactionTemplate(adminTransactionManager).execute(status -> {
            var galleryId = testITCTools.generateGalleryFromDirectory(ADMIN_ID);
            long pageId;
            try {
                pageId = testITCTools.generatePage();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            pageGalleryService.addPageGallery(pageId, galleryId, 0);
            return new long[]{ pageId, galleryId };
        });
        assertNotNull(ids);
        var before = Instant.now()
            .truncatedTo(ChronoUnit.SECONDS);

        var accepted = publishService.publishPageAsTask(ids[0]);
        awaitTaskCompletion(accepted.getTaskId());

        assertNotNull(pageService.findById(ids[0])
            .getPublished(), "the page records its release");
        assertNotNull(fileService.findGalleryById(ids[1])
            .getPublished(), "the gallery of the page records its release");
        assertFilesReleasedSince(galleryFileService.findGalleryFileByGalleryId(ids[1]), before);
    }

    private void assertFilesReleasedSince(java.util.List<fi.poltsi.vempain.admin.entity.file.GalleryFile> galleryFiles, Instant before) {
        assertFalse(galleryFiles.isEmpty());
        for (var galleryFile : galleryFiles) {
            var siteFile = siteFileRepository.findById(galleryFile.getSiteFileId())
                .orElseThrow();
            assertNotNull(siteFile.getPublished(), "file " + siteFile.getFileName() + " records its release");
            assertFalse(siteFile.getPublished()
                .isBefore(before), "the release time is the time of this publish");
        }
    }

    /** Waits until the background task has finished and asserts that it completed */
    private void awaitTaskCompletion(String taskId) throws InterruptedException {
        var deadline = Instant.now()
            .plusSeconds(30);
        var task = taskProgressStore.find(taskId)
            .orElseThrow();
        while (!task.isFinished()
               && Instant.now()
                   .isBefore(deadline)) {
            Thread.sleep(50);
            task = taskProgressStore.find(taskId)
                .orElseThrow();
        }
        assertTrue(task.isFinished(), "Task did not finish in time");
        assertEquals(fi.poltsi.vempain.common.api.TaskStatusEnum.COMPLETED, task.getStatus(), task.getErrorMessage());
    }
}
