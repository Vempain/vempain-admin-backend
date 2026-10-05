package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static fi.poltsi.vempain.admin.api.Constants.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
class PublishServiceITC extends AbstractITCTest {

	@Test
	void publishPageOk() {
		var testPageId = testSetup();

		try {
			var sitePageId = publishService.publishPage(testPageId);
			var optionalSitePage = webSitePageRepository.findById(sitePageId);
			assertTrue(optionalSitePage.isPresent());
			var sitePage = optionalSitePage.get();
			log.info("Site page: {}", sitePage);
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
		publishService.publishGallery(gallery.getId());
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
								 .isPresent() || webSitePageRepository.findByPageId(allowedPageId)
																	  .isPresent());
		assertTrue(webSitePageRepository.findByPageId(deniedPageId)
										.isEmpty());
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
