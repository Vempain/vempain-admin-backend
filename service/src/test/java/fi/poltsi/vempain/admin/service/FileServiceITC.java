package fi.poltsi.vempain.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.request.FileProcessRequest;
import fi.poltsi.vempain.admin.exception.VempainAclException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.exception.VempainFileExeption;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static fi.poltsi.vempain.admin.api.Constants.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class FileServiceITC extends AbstractITCTest {
	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void addFilesFromDirectoryWithGalleryOk() throws VempainEntityNotFoundException, IOException, VempainFileExeption, VempainAclException {
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

		var commonFiles = fileService.addFilesFromDirectory(request);
		assertNotNull(commonFiles);
		assertFalse(commonFiles.isEmpty());
		assertEquals(numberOfFiles, commonFiles.size());
		var    gallery     = fileService.findGalleryByShortname(galleryShortname);
		String galleryJson = objectMapper.writeValueAsString(gallery);
		log.info("Gallery: {}", galleryJson);
		assertEquals(ADMIN_ID, gallery.getCreator());
		assertEquals(numberOfFiles, gallery.getCommonFiles().size());

		for (var fileCommon1 : gallery.getCommonFiles()) {
			var optionalFileCommon = fileService.findCommonById(fileCommon1.getId());
			assertTrue(optionalFileCommon.isPresent());
			var fileCommon = optionalFileCommon.get();
			assertEquals(ADMIN_ID, fileCommon.getCreator());
		}
	}
}
