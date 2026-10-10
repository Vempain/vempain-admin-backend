package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.request.PagePagedRequest;
import fi.poltsi.vempain.admin.entity.Page;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertNull;

class PageServiceITC extends AbstractITCTest {
	@Test
	void findPagedByUserReturnsModifiedAscendingAcrossPages() throws Exception {
		var pages = savePagesWithModifiedTimes();
		var request = new PagePagedRequest();
		request.setPage(0);
		request.setSize(2);
		request.setSortBy("modified");
		request.setDirection(Sort.Direction.ASC);

		var response = pageService.findPagedByUser(request);

		assertEquals(3, response.getTotalElements());
		assertEquals(2, response.getTotalPages());
		assertEquals(List.of(pages.get(1)
		                          .getId(), pages.get(2)
		                                         .getId()), response.getContent()
																	.stream()
																	.map(page -> page.getId())
																	.toList());
	}

	@Test
	void findPagedByUserReturnsModifiedDescending() throws Exception {
		var pages = savePagesWithModifiedTimes();
		var request = new PagePagedRequest();
		request.setPage(0);
		request.setSize(25);
		request.setSortBy("modified");
		request.setDirection(Sort.Direction.DESC);

		var response = pageService.findPagedByUser(request);

		assertEquals(List.of(pages.get(0)
		                          .getId(), pages.get(2)
		                                         .getId(), pages.get(1)
		                                                        .getId()),
					 response.getContent()
							 .stream()
							 .map(page -> page.getId())
							 .toList());
	}

	private List<Page> savePagesWithModifiedTimes() throws Exception {
		var pageIds = List.of(testITCTools.generatePage(), testITCTools.generatePage(), testITCTools.generatePage());
		var pages = pageIds.stream()
						   .map(pageRepository::findById)
						   .map(java.util.Optional::orElseThrow)
						   .toList();
		pages.get(0)
		     .setModified(Instant.parse("2026-03-10T14:00:00Z"));
		pages.get(1)
		     .setModified(Instant.parse("2026-03-10T12:00:00Z"));
		pages.get(2)
		     .setModified(Instant.parse("2026-03-10T13:00:00Z"));
		pages.forEach(pageRepository::save);
		return pages;
	}

	@Test
	void suggestPathUsesOnlyReadablePagesAndTreatsWildcardsLiterally() throws Exception {
		// PageRepository.findById(long) returns the entity itself; the Optional overload would be picked for a boxed id
		var readable1 = pageRepository.findById(testITCTools.generatePage()
															.longValue());
		var readable2 = pageRepository.findById(testITCTools.generatePage()
															.longValue());
		var denied = pageRepository.findById(testITCTools.generatePage()
														 .longValue());
		var other = pageRepository.findById(testITCTools.generatePage()
														.longValue());
		readable1.setPagePath("/some/path/to/page");
		readable2.setPagePath("/some/path/also/page");
		denied.setPagePath("/some/secret/page");
		other.setPagePath("/elsewhere/page");
		// The denied page is readable only by another user, not by the administrator running the test
		var otherUser = testITCTools.generateUser();
		denied.setAclId(testITCTools.generateAclForOwnerOnly(otherUser, null, true, true, true, true));
		pageRepository.saveAll(List.of(readable1, readable2, denied, other));

		var response = pageService.suggestPath("/so");

		assertEquals(2, response.getMatches());
		assertEquals("/some/path", response.getSuggestion());

		// A single match suggests its parent path
		assertEquals("/some/path/also", pageService.suggestPath("/some/path/al")
												   .getSuggestion());

		// LIKE wildcards in the typed text are literal characters, not patterns
		var wildcard = pageService.suggestPath("%");
		assertEquals(0, wildcard.getMatches());
		assertNull(wildcard.getSuggestion());

		// Pages the caller may not read are ignored even when they are the only match
		var secret = pageService.suggestPath("/some/sec");
		assertEquals(0, secret.getMatches());
		assertNull(secret.getSuggestion());
	}
}
