package fi.poltsi.vempain.admin.entity.file;

import fi.poltsi.vempain.common.api.FileTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FileThumbUTC {
	@Test
	void toResponseCarriesTheThumbDimensionsAndItsSiteFile() {
		var siteFile = SiteFile.builder()
							   .id(7L)
							   .creator(1L)
							   .fileName("a.jpg")
							   .filePath("2024/trip")
							   .fileType(FileTypeEnum.IMAGE)
							   .build();
		var thumb = FileThumb.builder()
							 .id(3L)
							 .parentId(7L)
							 .parentType(FileTypeEnum.IMAGE)
							 .width(250L)
							 .height(167L)
							 .filename("a.jpeg")
							 .filepath("thumb/image/2024/trip")
							 .filesize(1234L)
							 .sha1sum("abc")
							 .build();
		thumb.setSiteFile(siteFile);

		var response = thumb.toResponse();

		assertEquals(3L, response.getId());
		assertEquals(7L, response.getParentId());
		assertEquals(FileTypeEnum.IMAGE, response.getParentType());
		assertEquals(250L, response.getWidth());
		assertEquals(167L, response.getHeight());
		assertNotNull(response.getSiteFile());
		assertEquals("a.jpg", response.getSiteFile()
									  .getFileName());
		assertEquals("2024/trip", response.getSiteFile()
										  .getFilePath());
	}
}
