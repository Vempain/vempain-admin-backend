package fi.poltsi.vempain.admin.entity;

import fi.poltsi.vempain.admin.api.response.PageResponse;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageUTC {
	private Page page;

	@BeforeEach
	void setUpOk() {
		page = TestUTCTools.generatePage(1L);
		assertNotNull(page);
	}

	@Test
	void getPageResponseOk() {
		PageResponse pageResponse = page.toResponse();
		assertEquals(1L, pageResponse.getFormId());
	}

	@Test
	void testToStringOk() {
		assertFalse(page.toString()
						.isEmpty());
	}

	@Test
	void getParentIdOk() {
		assertTrue(page.getParentId() > -1);
	}

	@Test
	void getFormIdOk() {
		assertTrue(page.getFormId() > 0);
	}

	@Test
	void getPathOk() {
		assertEquals("/index", page.getPagePath());
	}

	@Test
	void getSecureOk() {
		assertTrue(page.isSecure());
	}

	@Test
	void getIndexListOk() {
		assertFalse(page.isIndexList());
	}

	@Test
	void getTitleOk() {
		assertEquals("Test title", page.getTitle());
	}

	@Test
	void getHeaderOk() {
		assertEquals("Test header", page.getHeader());
	}

	@Test
	void getBodyOk() {
		assertEquals("This is test body", page.getBody());
	}

	@Test
	void setParentIdOk() {
		page.setParentId(1L);
		assertEquals(1L, page.getParentId());
	}

	@Test
	void setFormIdOk() {
		page.setFormId(2L);
		assertEquals(2L, page.getFormId());
	}

	@Test
	void setPathOk() {
		page.setPagePath("/index2");
		assertEquals("/index2", page.getPagePath());
	}

	@Test
	void setSecureOk() {
		page.setSecure(false);
		assertFalse(page.isSecure());
	}

	@Test
	void setIndexListOk() {
		page.setIndexList(true);
		assertTrue(page.isIndexList());
	}

	@Test
	void setTitleOk() {
		page.setTitle("Test new title");
		assertEquals("Test new title", page.getTitle());
	}

	@Test
	void setHeaderOk() {
		page.setHeader("Test new header");
		assertEquals("Test new header", page.getHeader());
	}

	@Test
	void setBodyOk() {
		page.setBody("This is a new test body");
		assertEquals("This is a new test body", page.getBody());
	}

	@Test
	void testEqualsOk() {
		Page clone = TestUTCTools.deepCopy(page, Page.class);
		boolean equals = page.equals(clone);
		assertTrue(equals);
	}

	@Test
	void canEqualOk() {
		Page clone = TestUTCTools.deepCopy(page, Page.class);
		assertTrue(page.canEqual(clone));
	}

	@Test
	void testHashCodeOk() {
		Page clone = TestUTCTools.deepCopy(page, Page.class);
		assertNotNull(clone);
		assertEquals(page.hashCode(), clone.hashCode());
	}

	@Test
	void allArgsConstructorOk() {
		Page newPage = new Page(0L, 1L, "/index", true, true, "Test title", "Test header", "Test body", null);
		assertNull(newPage.getCreator());
	}
}
