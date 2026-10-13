package fi.poltsi.vempain.admin.entity.file;

import fi.poltsi.vempain.common.api.FileTypeEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SiteFileUTC {
	private static GpsLocation location() {
		return GpsLocation.builder()
						  .id(9L)
						  .latitude(new BigDecimal("60.1"))
						  .latitudeRef('N')
						  .longitude(new BigDecimal("24.9"))
						  .longitudeRef('E')
						  .city("Helsinki")
						  .build();
	}

	private static SiteFile sample(GpsLocation location, Integer width, Integer height) {
		return SiteFile.builder()
					   .id(5L)
					   .aclId(50L)
					   .creator(1L)
					   .modifier(2L)
					   .fileId(500L)
					   .fileName("a.jpg")
					   .filePath("2024/trip")
					   .fileType(FileTypeEnum.IMAGE)
					   .mimeType("image/jpeg")
					   .size(1024L)
					   .sha256sum("deadbeef")
					   .comment("A comment")
					   .width(width)
					   .height(height)
					   .length(Duration.ofSeconds(3))
					   .pages(1)
					   .metadata("{\"a\":1}")
					   .originalDateTime(Instant.parse("2024-06-01T10:00:00Z"))
					   .rightsHolder("holder")
					   .rightsTerms("terms")
					   .rightsUrl("https://example.org/rights")
					   .creatorName("creator")
					   .creatorEmail("creator@example.org")
					   .creatorCountry("FI")
					   .creatorUrl("https://example.org")
					   .location(location)
					   .build();
	}

	@Test
	void toWebSiteFileCopiesTheMetadataAndMapsTheLocation() {
		var web = sample(location(), 800, 600).toWebSiteFile();

		assertEquals(5L, web.getFileId());
		assertEquals(50L, web.getAclId());
		assertEquals("A comment", web.getComment());
		assertEquals("image/jpeg", web.getMimetype());
		assertEquals(FileTypeEnum.IMAGE, web.getFileType());
		assertEquals(Instant.parse("2024-06-01T10:00:00Z"), web.getOriginalDateTime());
		assertEquals("holder", web.getRightsHolder());
		assertEquals("terms", web.getRightsTerms());
		assertEquals("https://example.org/rights", web.getRightsUrl());
		assertEquals("creator", web.getCreatorName());
		assertEquals("creator@example.org", web.getCreatorEmail());
		assertEquals("FI", web.getCreatorCountry());
		assertEquals("https://example.org", web.getCreatorUrl());
		assertEquals(800, web.getWidth());
		assertEquals(600, web.getHeight());
		assertEquals(Duration.ofSeconds(3), web.getLength());
		assertEquals(1, web.getPages());
		assertEquals("{\"a\":1}", web.getMetadata());
		assertNotNull(web.getLocation());
		assertEquals(9L, web.getLocation()
							.getId());
		assertEquals("Helsinki", web.getLocation()
									.getCity());
	}

	@Test
	void toWebSiteFileDefaultsMissingDimensionsToZeroAndKeepsALocationlessFile() {
		var web = sample(null, null, null).toWebSiteFile();

		assertEquals(0, web.getWidth());
		assertEquals(0, web.getHeight());
		assertNull(web.getLocation());
	}

	@Test
	void toResponseMapsTheLocationWhenPresent() {
		var withLocation = sample(location(), 800, 600).toResponse();
		assertNotNull(withLocation.getLocation());
		assertEquals(9L, withLocation.getLocation()
									 .getId());
		assertEquals("a.jpg", withLocation.getFileName());
		assertEquals(500L, withLocation.getFileId());

		assertNull(sample(null, 1, 1).toResponse()
									 .getLocation());
	}
}
