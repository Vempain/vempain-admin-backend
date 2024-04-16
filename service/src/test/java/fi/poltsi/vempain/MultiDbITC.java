package fi.poltsi.vempain;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.entity.Page;
import fi.poltsi.vempain.admin.repository.PageRepository;
import fi.poltsi.vempain.site.entity.SitePage;
import fi.poltsi.vempain.site.repository.SitePageRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class MultiDbITC extends AbstractITCTest {
	@Autowired
	private PageRepository     adminPageRepository;
	@Autowired
	private SitePageRepository sitePageRepository;

	@Test
	void copyFromAdminToSite() {
		setupTests();
		var fetchAdminPage = adminPageRepository.findById(1L);
		assertNotNull(fetchAdminPage);
		var sitePage = SitePage.builder()
							   .id(fetchAdminPage.getId())
							   .body(fetchAdminPage.getBody())
							   .header(fetchAdminPage.getHeader())
							   .parentId(fetchAdminPage.getParentId())
							   .path(fetchAdminPage.getPath())
							   .indexList(fetchAdminPage.isIndexList())
							   .secure(fetchAdminPage.isSecure())
							   .title(fetchAdminPage.getTitle())
							   .creator("Erkki")
							   .created(fetchAdminPage.getCreated())
							   .modifier("Erkki")
							   .modified(fetchAdminPage.getModified())
							   .published(Instant.now())
							   .build();
		sitePageRepository.save(sitePage);
		var sitePages = sitePageRepository.findAll();
		assertNotNull(sitePages);
		assertEquals(1, StreamSupport.stream(sitePages.spliterator(), false).count());
	}

	private void setupTests() {
		var userId = testITCTools.generateUser();
		var aclId  = testITCTools.generateAcl(userId, null, true, true, true, true);
		var formId = testITCTools.generateForm();
		var adminPage = Page.builder()
							.aclId(aclId)
							.formId(formId)
							.header("Test header")
							.title("Test title")
							.path("/test/path")
							.body("<html><body>Test body</body></html>")
							.indexList(false)
							.locked(false)
							.secure(false)
							.creator(userId)
							.created(Instant.now())
							.modifier(null)
							.modified(null)
							.build();
		adminPageRepository.save(adminPage);
		var pages = adminPageRepository.findAll();
		assertNotNull(pages);
		assertEquals(1, StreamSupport.stream(pages.spliterator(), false).count());
	}
}
