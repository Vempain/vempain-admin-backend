package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.request.PagePagedRequest;
import fi.poltsi.vempain.admin.entity.Page;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
