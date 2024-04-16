package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
class PublishServiceITC extends AbstractITCTest {
	private Long pageId;

	@Test
	void publishOk() {
		testSetup();

		try {
			publishService.publishPage(pageId);
			var optionalSitePage = sitePageRepository.findById(pageId);
			assertTrue(optionalSitePage.isPresent());
			var sitePage = optionalSitePage.get();
			log.info("Site page: {}", sitePage);
		} catch (VempainEntityNotFoundException e) {
			fail("Should not have received VempainEntityNotFoundException when publishing page", e);
		}
	}

	@Test
	void deleteOk() {
		testSetup();
		publishService.deletePage(pageId);
		var optionalSitePage = publishService.fetchSitePage(pageId);
		assertTrue(optionalSitePage.isEmpty());
	}

	@Test
	void publishGalleryOk() throws VempainEntityNotFoundException {
		var userId = testITCTools.generateUser();
		var galleryId = testITCTools.generateGalleryFromDirectory(userId);
		publishService.publishGallery(galleryId);
	}

	@Test
	void dateParsingTest() {
		var datetimeString   = "2017:06:09 13:23:30+02:00";
		var referenceDateTime = Instant.parse("2017-06-09T11:23:30Z");
		var dtm = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssXXX").withZone(ZoneId.systemDefault());
		var originalDateTime = dtm.parse(datetimeString, Instant::from);
		assertEquals(referenceDateTime, originalDateTime);
	}

	void testSetup() {
		try {
			pageId = testITCTools.generatePage();
			log.info("Created test page with ID: {}", pageId);
		} catch (Exception e) {
			log.error("Failed to create a page:", e);
		}

		var checkPage = pageRepository.findById(pageId);
		assertTrue(checkPage.isPresent());
	}
}
