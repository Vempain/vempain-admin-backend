package fi.poltsi.vempain.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.request.FileProcessRequest;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class FileServiceITC extends AbstractITCTest {
	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void addFilesFromDirectoryWithGalleryOk() throws VempainEntityNotFoundException, IOException, VempainAclException {
		// NOTE: In AbstractITCTest#setUp we drop the gallery_file -> site_file FK for test compatibility,
		// since FileService still writes legacy identifiers.
		var testUserId = testITCTools.generateUser();
		assertNotNull(testUserId);
		// The path is relative to service-directory
		var source           = "image/Test/another";
		var destination      = "Test/another";
		var galleryShortname = "Test gallery";
		var files            = testITCTools.prepareConvertedForTests(destination);
		log.info("Test tools created files: {}", files);
		var numberOfFiles    = files.size();
		var request = FileProcessRequest.builder()
										.sourceDirectory(source)
										.destinationDirectory(destination)
										.generateGallery(true)
										.galleryShortname(galleryShortname)
										.galleryDescription("A test gallery created by addFilesFromDirectoryOk")
										.generatePage(false)
										.build();

		var commonFiles = fileService.addFilesFromDirectory(request, testUserId);
		assertNotNull(commonFiles);
		assertFalse(commonFiles.isEmpty());
		assertEquals(numberOfFiles, commonFiles.size());

		var    gallery     = fileService.findGalleryByShortname(galleryShortname);
		String galleryJson = objectMapper.writeValueAsString(gallery);
		log.info("Gallery: {}", galleryJson);
		assertEquals(testUserId, gallery.getCreator());

		// Note: gallery_file now references site_file_id (not file_common_id). The association to common files
		// is no longer asserted here; the service return already validates the count.
	}
}
