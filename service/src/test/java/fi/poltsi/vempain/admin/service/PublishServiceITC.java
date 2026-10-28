package fi.poltsi.vempain.admin.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.service.file.GalleryFileService;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.site.repository.WebSiteFileRepository;
import fi.poltsi.vempain.site.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.site.service.SiteDatabaseRetry;
import lombok.extern.slf4j.Slf4j;

import static fi.poltsi.vempain.admin.api.Constants.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
class PublishServiceITC extends AbstractITCTest {

    @Autowired
    private GalleryFileService galleryFileService;
    @Autowired
    private SiteDatabaseRetry siteDatabaseRetry;
    @Autowired
    private WebSiteFileRepository webSiteFileRepository;
    @Autowired
    private WebSiteGalleryRepository webSiteGalleryRepository;
    @Autowired
    @Qualifier("siteDataSource")
    private DataSource siteDataSource;

    @Test
    void publishPageOk() {
        var testPageId = testSetup();
        assertNull(pageService.findById(testPageId)
            .getPublished(), "A new page has not been released to the website");

        try {
            var sitePageId = publishService.publishPage(testPageId);
            var optionalSitePage = webSitePageRepository.findById(sitePageId);
            assertTrue(optionalSitePage.isPresent());
            var sitePage = optionalSitePage.get();
            log.info("Site page: {}", sitePage);
            assertNotNull(pageService.findById(testPageId)
                .getPublished(), "The page records when it was released to the website");
        } catch (VempainEntityNotFoundException e) {
            fail("Should not have received VempainEntityNotFoundException when publishing page", e);
        }
    }

    @Test
    void deletePageOk() {
        var pageId = testSetup();
        publishService.deletePage(pageId);
        var optionalSitePage = publishService.fetchSitePage(pageId);
        assertTrue(optionalSitePage.isEmpty());
    }

    @Test
    void publishGalleryOk() throws VempainEntityNotFoundException {
        var gallery = fileService.createEmptyGallery("Publish test gallery", "A test gallery", ADMIN_ID);
        assertNull(gallery.getPublished(), "A new gallery has not been released to the website");
        publishService.publishGallery(gallery.getId());
        // An empty gallery transfers nothing to the website, so it is still not released
        assertNull(fileService.findGalleryById(gallery.getId())
            .getPublished());
    }

    @Test
    void publishPageIsDeniedWhenUserLacksModifyOnPage() {
        var pageId = testSetup();
        var page = pageRepository.findById(pageId);
        // Replace the page ACL with one that excludes the administrator who is running the test
        var strangerId = testITCTools.generateUser();
        page.setAclId(testITCTools.generateAclForOwnerOnly(strangerId, null, true, true, true, true));
        pageRepository.save(page);

        assertThrows(AccessDeniedException.class, () -> publishService.publishPage(pageId));
        assertTrue(publishService.fetchSitePage(pageId)
            .isEmpty());
    }

    @Test
    void publishPageIsDeniedWhenUserMayNotReadTheForm() {
        var pageId = testSetup();
        var page = pageRepository.findById(pageId);
        var form = formRepository.findById(page.getFormId())
            .orElseThrow();
        var strangerId = testITCTools.generateUser();
        form.setAclId(testITCTools.generateAclForOwnerOnly(strangerId, null, true, true, true, true));
        formRepository.save(form);

        assertThrows(AccessDeniedException.class, () -> publishService.publishPage(pageId));
        assertFalse(publishService.canPublishPage(page));
    }

    @Test
    void publishAllPagesSkipsPagesTheUserMayNotPublish() throws VempainEntityNotFoundException {
        var allowedPageId = testSetup();
        var deniedPageId = testSetup();
        var denied = pageRepository.findById(deniedPageId);
        var strangerId = testITCTools.generateUser();
        denied.setAclId(testITCTools.generateAclForOwnerOnly(strangerId, null, true, true, true, true));
        pageRepository.save(denied);

        publishService.publishAllPages();

        assertTrue(publishService.fetchSitePage(allowedPageId)
            .isPresent()
                   || webSitePageRepository.findByPageId(allowedPageId)
                       .isPresent());
        assertTrue(webSitePageRepository.findByPageId(deniedPageId)
            .isEmpty());
    }

    @Test
    void writingAGalleryToTheSiteAgainUpdatesItsRowsInPlace() throws VempainEntityNotFoundException {
        var galleryId = testITCTools.generateGalleryFromDirectory(ADMIN_ID);
        var gallery = fileService.findGalleryById(galleryId);
        var galleryFiles = galleryFileService.findGalleryFileByGalleryId(galleryId);
        assertFalse(galleryFiles.isEmpty());
        var siteJdbc = new JdbcTemplate(siteDataSource);

        // The site part of the publish, run like the publish runs it: in one site transaction of its own
        var firstSiteGalleryId = siteDatabaseRetry.execute("first",
                () -> publishService.writeSiteGallery(gallery, galleryFiles, Instant.now(), TagBlacklist.EMPTY, "admin", null));
        var firstFileRows = siteFileRowsOf(gallery);
        assertEquals(galleryFiles.size(), firstFileRows.size());
        assertEquals(galleryFiles.size(), countGalleryFiles(siteJdbc, firstSiteGalleryId));

        // The second write of the same gallery (a republish) finds every row on the site: the image thumbnail paths are unique there
        var secondSiteGalleryId = siteDatabaseRetry.execute("second",
                () -> publishService.writeSiteGallery(gallery, galleryFiles, Instant.now(), TagBlacklist.EMPTY, "admin", "admin"));

        assertEquals(firstSiteGalleryId, secondSiteGalleryId, "The site gallery is updated in place");
        assertEquals(1, webSiteGalleryRepository.findAllByGalleryIdOrderByIdAsc(galleryId)
            .size());
        assertEquals("admin", webSiteGalleryRepository.findById(secondSiteGalleryId)
            .orElseThrow()
            .getModifier());
        var secondFileRows = siteFileRowsOf(gallery);
        assertEquals(firstFileRows.keySet(), secondFileRows.keySet(), "Every file keeps its site id");
        for (var siteFile : gallery.getSiteFiles()) {
            var rows = webSiteFileRepository.findByFileIdOrFilePathOrThumbnailPathOrderByIdAsc(siteFile.getId(), null, null);
            assertEquals(1, rows.size(), "One site row per file");
            assertEquals(firstFileRows.get(rows.getFirst()
                .getId()), rows.getFirst()
                    .getAclId(), "The site ACL of the file is kept");
        }
        assertEquals(galleryFiles.size(), countGalleryFiles(siteJdbc, secondSiteGalleryId), "The file relations are written once");
    }

    /**
     * @return the site ACL by site file id of every file of the gallery
     */
    private Map<Long, Long> siteFileRowsOf(fi.poltsi.vempain.admin.entity.file.Gallery gallery) {
        var rows = new HashMap<Long, Long>();
        for (var siteFile : gallery.getSiteFiles()) {
            for (var row : webSiteFileRepository.findByFileIdOrFilePathOrThumbnailPathOrderByIdAsc(siteFile.getId(), null, null)) {
                rows.put(row.getId(), row.getAclId());
            }
        }
        return rows;
    }

    private static long countGalleryFiles(JdbcTemplate siteJdbc, long siteGalleryId) {
        var count = siteJdbc.queryForObject("SELECT count(*) FROM web_site_gallery_file WHERE gallery_id = ?", Long.class, siteGalleryId);
        return count == null ? 0 : count;
    }

    @Test
    void publishGalleryIsDeniedWhenAFileIsNotReadable() {
        var galleryId = testITCTools.generateGalleryFromDirectory(ADMIN_ID);
        var gallery = fileService.findGalleryById(galleryId);
        var strangerId = testITCTools.generateUser();
        var hiddenFile = gallery.getSiteFiles()
            .getFirst();
        hiddenFile.setAclId(testITCTools.generateAclForOwnerOnly(strangerId, null, true, true, true, true));
        siteFileRepository.save(hiddenFile);

        assertFalse(publishService.canPublishGallery(galleryId));
        assertThrows(AccessDeniedException.class, () -> publishService.publishGallery(galleryId));
    }

    @Test
    void dateParsingTest() {
        var datetimeString = "2017:06:09 13:23:30+02:00";
        var referenceDateTime = Instant.parse("2017-06-09T11:23:30Z");
        var dtm = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssXXX")
            .withZone(ZoneId.systemDefault());
        var originalDateTime = dtm.parse(datetimeString, Instant::from);
        assertEquals(referenceDateTime, originalDateTime);
    }

    private long testSetup() {
        var pageId = 0L;

        try {
            pageId = testITCTools.generatePage();
            assertNotNull(pageId);
            log.info("Created test page with ID: {}", pageId);
        } catch (Exception e) {
            log.error("Failed to create a page:", e);
        }

        var checkPage = pageRepository.findById(pageId);
        assertNotNull(checkPage);
        return pageId;
    }
}
