package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.request.PageRequest;
import fi.poltsi.vempain.admin.entity.Page;
import fi.poltsi.vempain.admin.exception.ProcessingFailedException;
import fi.poltsi.vempain.admin.repository.PageRepository;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.service.AclService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageServiceUTC {
	@Mock
	PageRepository pageRepository;
	@Mock
	AclService     aclService;
	@Mock
	AccessService  accessService;

	@InjectMocks
	private PageService pageService;

	@Test
	void findAllOk() {
		List<Page> pages = TestUTCTools.generatePageList(10L);
		when(pageRepository.findAll()).thenReturn(pages);

		try {
			Iterable<Page> returnPages = pageService.findAll();
			assertNotNull(returnPages);
			assertEquals(10L, StreamSupport.stream(returnPages.spliterator(), false)
										   .count());
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void findAllByUserOk() {
		List<Page> pages = TestUTCTools.generatePageList(10L);
		when(pageRepository.findAll()).thenReturn(pages);
		when(accessService.hasReadPermission(anyLong())).thenReturn(true);

		try {
			Iterable<Page> returnPages = pageService.findAllByUser();
			assertNotNull(returnPages);
			assertEquals(10L, StreamSupport.stream(returnPages.spliterator(), false)
										   .count());
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void findAllByUserNoAccessOk() {
		List<Page> pages = TestUTCTools.generatePageList(10L);
		when(pageRepository.findAll()).thenReturn(pages);
		when(accessService.hasReadPermission(anyLong())).thenReturn(false);

		try {
			Iterable<Page> returnPages = pageService.findAllByUser();
			assertNotNull(returnPages);
			assertEquals(0L, StreamSupport.stream(returnPages.spliterator(), false)
										  .count());
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void findByIdOk() {
		Page page = TestUTCTools.generatePage(1L);
		when(pageRepository.findById(1L)).thenReturn(page);

		try {
			Page response = pageService.findById(1L);
			assertNotNull(response);
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void findByPathOk() {
		Page page = TestUTCTools.generatePage(1L);
		when(pageRepository.findByPagePath("/index")).thenReturn(page);

		try {
			Page response = pageService.findByPath("/index");
			assertNotNull(response);
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void findByPathNoResultFail() {
		when(pageRepository.findByPagePath("/index")).thenReturn(null);

		try {
			pageService.findByPath("/index");
			fail("Should have received EntityNotFoundException exception when trying to find a page by non-existing path");
		} catch (VempainEntityNotFoundException e) {
			assertEquals("Failed to find page by path", e.getMessage());
			assertEquals("page", e.getEntityName());
		}
	}

	@Test
	void saveOk() {
		Page page = TestUTCTools.generatePage(1L);
		when(pageRepository.save(page)).thenReturn(page);

		try {
			Page response = pageService.save(page);
			assertEquals(page, response);
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void saveFromPageRequestOk() {
		Page page = TestUTCTools.generatePage(1L);
		PageRequest request = TestUTCTools.generatePageRequestFromPage(page);
		when(accessService.getValidUserId()).thenReturn(1L);
		when(pageRepository.findByPagePath(request.getPath())).thenReturn(null);
		when(aclService.saveNewAclForObject(request.getAcls())).thenReturn(1L);
		when(pageRepository.save(any())).thenReturn(page);

		try {
			Page response = pageService.saveFromPageRequest(request);
			assertNotNull(response);
			assertEquals(request.getHeader(), response.getHeader());
			assertEquals(request.getTitle(), response.getTitle());
			assertEquals(request.getBody(), response.getBody());
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void saveFromPageRequestInvalidUserSessionFail() {
		Page page = TestUTCTools.generatePage(1L);
		PageRequest request = TestUTCTools.generatePageRequestFromPage(page);
		doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Test exception")).when(accessService)
																					   .getValidUserId();

		try {
			pageService.saveFromPageRequest(request);
			fail("Should have caught a SessionAuthenticationException");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"Test exception\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void saveFromPageRequestPathExistsFail() {
		Page page = TestUTCTools.generatePage(1L);
		PageRequest request = TestUTCTools.generatePageRequestFromPage(page);
		when(accessService.getValidUserId()).thenReturn(1L);
		when(pageRepository.findByPagePath(request.getPath())).thenReturn(page);

		try {
			pageService.saveFromPageRequest(request);
			fail("Should have caught a ResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals("409 CONFLICT \"" + VempainMessages.OBJECT_NAME_ALREADY_EXISTS + "\"", e.getMessage());
			assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void updateFromRequestOk() throws VempainAclException {
		Page page = TestUTCTools.generatePage(1L);
		PageRequest request = TestUTCTools.generatePageRequestFromPage(page);
		when(accessService.getValidUserId()).thenReturn(1L);
		when(pageRepository.findById(request.getId())).thenReturn(page);
		when(accessService.hasModifyPermission(page.getAclId())).thenReturn(true);
		doNothing().when(aclService)
				   .updateFromRequestList(request.getAcls());
		when(pageRepository.save(any())).thenReturn(page);

		try {
			Page response = pageService.updateFromRequest(request);
			assertNotNull(response);
			assertEquals(request.getHeader(), response.getHeader());
			assertEquals(request.getTitle(), response.getTitle());
			assertEquals(request.getBody(), response.getBody());
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void updateFromRequestNoSessionFail() {
		Page page = TestUTCTools.generatePage(1L);
		PageRequest request = TestUTCTools.generatePageRequestFromPage(page);
		doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Test exception")).when(accessService)
																					   .getValidUserId();

		try {
			pageService.updateFromRequest(request);
			fail("We should not have been able to update page with no session");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"Test exception\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void updateFromRequestNoPageFoundFail() {
		Page page = TestUTCTools.generatePage(1L);
		PageRequest request = TestUTCTools.generatePageRequestFromPage(page);
		when(accessService.getValidUserId()).thenReturn(1L);
		when(pageRepository.findById(request.getId())).thenReturn(null);

		try {
			pageService.updateFromRequest(request);
			fail("We should not have been able to update a non-existing page");
		} catch (ResponseStatusException e) {
			assertEquals("404 NOT_FOUND \"" + VempainMessages.OBJECT_NOT_FOUND + "\"", e.getMessage());
			assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void updateFromRequestNoPermissionsFail() {
		Page page = TestUTCTools.generatePage(1L);
		PageRequest request = TestUTCTools.generatePageRequestFromPage(page);
		when(accessService.getValidUserId()).thenReturn(1L);
		when(pageRepository.findById(request.getId())).thenReturn(page);
		when(accessService.hasModifyPermission(page.getAclId())).thenReturn(false);

		try {
			pageService.updateFromRequest(request);
			fail("We should not have been able to update a page without proper permissions");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"" + VempainMessages.UNAUTHORIZED_ACCESS + "\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void updateFromRequestUpdatedPathAlreadyExistsFail() {
		Page page = TestUTCTools.generatePage(1L);
		PageRequest request = TestUTCTools.generatePageRequestFromPage(page);
		when(accessService.getValidUserId()).thenReturn(1L);
		when(pageRepository.findById(request.getId())).thenReturn(page);
		when(accessService.hasModifyPermission(page.getAclId())).thenReturn(true);
		request.setPath("/new-path");
		Page page2 = TestUTCTools.generatePage(2L);
		page2.setPagePath("/new-path");
		when(pageRepository.findByPagePath(request.getPath())).thenReturn(page2);

		try {
			pageService.updateFromRequest(request);
			fail("We should not have been able to update a page path which already exists");
		} catch (ResponseStatusException e) {
			assertEquals("400 BAD_REQUEST \"" + VempainMessages.MALFORMED_OBJECT_IN_REQUEST + "\"", e.getMessage());
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void updateFromRequestUpdatedPathOk() throws VempainAclException {
		Page page = TestUTCTools.generatePage(1L);
		PageRequest request = TestUTCTools.generatePageRequestFromPage(page);
		when(accessService.getValidUserId()).thenReturn(1L);
		when(pageRepository.findById(request.getId())).thenReturn(page);
		when(accessService.hasModifyPermission(page.getAclId())).thenReturn(true);
		request.setPath("/new-path");
		when(pageRepository.findByPagePath(request.getPath())).thenReturn(page);
		when(accessService.hasModifyPermission(page.getAclId())).thenReturn(true);
		doNothing().when(aclService)
				   .updateFromRequestList(request.getAcls());
		when(pageRepository.save(any())).thenReturn(page);

		try {
			Page response = pageService.updateFromRequest(request);
			assertNotNull(response);
			assertEquals(request.getHeader(), response.getHeader());
			assertEquals(request.getTitle(), response.getTitle());
			assertEquals(request.getBody(), response.getBody());
			assertEquals(request.getPath(), response.getPagePath());
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void updateFromRequestUpdatedPathNoPageFoundOk() throws VempainAclException {
		Page page = TestUTCTools.generatePage(1L);
		PageRequest request = TestUTCTools.generatePageRequestFromPage(page);
		when(accessService.getValidUserId()).thenReturn(1L);
		when(pageRepository.findById(request.getId())).thenReturn(page);
		when(accessService.hasModifyPermission(page.getAclId())).thenReturn(true);
		request.setPath("/new-path");
		when(pageRepository.findByPagePath(request.getPath())).thenReturn(null);
		when(accessService.hasModifyPermission(page.getAclId())).thenReturn(true);
		doNothing().when(aclService)
				   .updateFromRequestList(request.getAcls());
		when(pageRepository.save(any())).thenReturn(page);

		try {
			Page response = pageService.updateFromRequest(request);
			assertNotNull(response);
			assertEquals(request.getHeader(), response.getHeader());
			assertEquals(request.getTitle(), response.getTitle());
			assertEquals(request.getBody(), response.getBody());
			assertEquals(request.getPath(), response.getPagePath());
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void updateFromRequestACLExceptionFail() throws VempainAclException {
		Page page = TestUTCTools.generatePage(1L);
		PageRequest request = TestUTCTools.generatePageRequestFromPage(page);
		when(accessService.getValidUserId()).thenReturn(1L);
		when(pageRepository.findById(request.getId())).thenReturn(page);
		when(accessService.hasModifyPermission(page.getAclId())).thenReturn(true);
		when(accessService.hasModifyPermission(page.getAclId())).thenReturn(true);
		doThrow(new VempainAclException("Test exception")).when(aclService)
														  .updateFromRequestList(request.getAcls());

		try {
			pageService.updateFromRequest(request);
			fail("We should not have been able to update a page when ACL fails");
		} catch (ResponseStatusException e) {
			assertEquals("500 INTERNAL_SERVER_ERROR \"" + VempainMessages.INTERNAL_ERROR + "\"", e.getMessage());
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void updateFromRequestSaveExceptionFail() throws VempainAclException {
		Page page = TestUTCTools.generatePage(1L);
		PageRequest request = TestUTCTools.generatePageRequestFromPage(page);
		when(accessService.getValidUserId()).thenReturn(1L);
		when(pageRepository.findById(request.getId())).thenReturn(page);
		when(accessService.hasModifyPermission(page.getAclId())).thenReturn(true);
		when(accessService.hasModifyPermission(page.getAclId())).thenReturn(true);
		doNothing().when(aclService)
				   .updateFromRequestList(request.getAcls());
		doThrow(new RuntimeException("Test exception")).when(pageRepository)
													   .save(any());

		try {
			pageService.updateFromRequest(request);
			fail("We should not have been able to update a page when save fails");
		} catch (ResponseStatusException e) {
			assertEquals("500 INTERNAL_SERVER_ERROR \"" + VempainMessages.INTERNAL_ERROR + "\"", e.getMessage());
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void deleteByIdOk() throws VempainEntityNotFoundException {
		Page page = TestUTCTools.generatePage(1L);
		when(pageRepository.findById(1L)).thenReturn(page);
		when(accessService.hasDeletePermission(1L)).thenReturn(true);
		doNothing().when(aclService)
				   .deleteByAclId(1L);

		try {
			pageService.deleteById(1L);
		} catch (Exception e) {
			fail("Should not have received an exception when deleting a page by ID " + e);
		}
	}

	@Test
	void deleteByIdNotFoundFail() {
		when(pageRepository.findById(1L)).thenReturn(null);

		try {
			pageService.deleteById(1L);
			fail("Should have received VempainEntityNotFoundException exception when trying to delete non-existing page ID");
		} catch (VempainEntityNotFoundException e) {
			assertEquals("Fail to delete a page with non-existing ID", e.getMessage());
		} catch (Exception e) {
			fail("Should not have received other exception besides ResourceNotFoundException");
		}
	}

	@Test
	void deleteByIdNoAccessFail() {
		Page page = TestUTCTools.generatePage(1L);
		when(pageRepository.findById(1L)).thenReturn(page);
		when(accessService.hasDeletePermission(1L)).thenReturn(false);

		try {
			pageService.deleteById(1L);
			fail("We should not have been able to delete the page without delete permission");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"Unauthorized access tried to delete page\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("We should only have received a ResponseStatusException: " + e);
		}
	}

	@Test
	void deleteByIdAClDeleteFail() throws VempainEntityNotFoundException {
		Page page = TestUTCTools.generatePage(1L);
		when(pageRepository.findById(1L)).thenReturn(page);
		when(accessService.hasDeletePermission(1L)).thenReturn(true);
		doThrow(new VempainEntityNotFoundException("ACL not found for deletion", "acl")).when(aclService)
																						.deleteByAclId(1L);

		try {
			pageService.deleteById(1L);
			fail("We should not have been able to delete the page when deleting the ACL fail");
		} catch (ProcessingFailedException e) {
			assertEquals("Failed to delete ACL", e.getMessage());
		} catch (Exception e) {
			fail("We should only have received a ProcessingFailedException: " + e);
		}
	}

	@Test
	void deleteByIdPageDeleteFail() throws VempainEntityNotFoundException {
		Page page = TestUTCTools.generatePage(1L);
		when(pageRepository.findById(1L)).thenReturn(page);
		when(accessService.hasDeletePermission(1L)).thenReturn(true);
		doNothing().when(aclService)
				   .deleteByAclId(1L);
		doThrow(new RuntimeException()).when(pageRepository)
									   .delete(page);

		try {
			pageService.deleteById(1L);
			fail("We should not have been able to delete the page when deleting the page caused an exception");
		} catch (ProcessingFailedException e) {
			assertEquals("Failed to delete page", e.getMessage());
		} catch (Exception e) {
			fail("We should only have received a ProcessingFailedException: " + e);
		}
	}

	@Test
	void deleteByIdNoSessionFail() {
		doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Test exception")).when(accessService)
																					   .getValidUserId();

		try {
			pageService.deleteById(1L);
			fail("We should not have been able to delete the page with no session");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"Test exception\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void deleteByUserOk() throws VempainEntityNotFoundException {
		when(accessService.getValidUserId()).thenReturn(1L);
		Page page = TestUTCTools.generatePage(1L);
		when(pageRepository.findById(1L)).thenReturn(page);
		when(accessService.hasDeletePermission(1L)).thenReturn(true);
		doNothing().when(aclService)
				   .deleteByAclId(1L);

		try {
			pageService.deleteByUser(1L);
		} catch (Exception e) {
			fail("Should not have received an exception when deleting a page by ID " + e);
		}
	}

	@Test
	void deleteByUserNoSessionFail() {
		doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Test exception")).when(accessService)
																					   .getValidUserId();

		try {
			pageService.deleteByUser(1L);
			fail("Should not have been able to delete without a session");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"Test exception\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void deleteByUserNoPageFail() {
		when(accessService.getValidUserId()).thenReturn(1L);
		when(pageRepository.findById(1L)).thenReturn(null);

		try {
			pageService.deleteByUser(1L);
			fail("Should not have been able to delete an non-existing page");
		} catch (ResponseStatusException e) {
			assertEquals("404 NOT_FOUND \"" + VempainMessages.OBJECT_NOT_FOUND + "\"", e.getMessage());
			assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void deleteByUserNoPermissionFail() {
		when(accessService.getValidUserId()).thenReturn(1L);
		Page page = TestUTCTools.generatePage(1L);
		when(pageRepository.findById(1L)).thenReturn(page);
		when(accessService.hasDeletePermission(1L)).thenReturn(false);

		try {
			pageService.deleteByUser(1L);
			fail("Should not have been able to delete without permission");
		} catch (ResponseStatusException e) {
			assertEquals("401 UNAUTHORIZED \"" + VempainMessages.UNAUTHORIZED_ACCESS + "\"", e.getMessage());
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void deleteByUserNoAclOk() throws VempainEntityNotFoundException {
		when(accessService.getValidUserId()).thenReturn(1L);
		Page page = TestUTCTools.generatePage(1L);
		when(pageRepository.findById(1L)).thenReturn(page);
		when(accessService.hasDeletePermission(1L)).thenReturn(true);
		doThrow(new VempainEntityNotFoundException("ACL not found for deletion", "acl")).when(aclService)
																						.deleteByAclId(page.getAclId());

		try {
			pageService.deleteByUser(1L);
		} catch (Exception e) {
			fail("Should not have received an exception when deleting a page by ID " + e);
		}
	}

	@Test
	void deleteByUserUnknownAclExceptionFail() throws VempainEntityNotFoundException {
		when(accessService.getValidUserId()).thenReturn(1L);
		Page page = TestUTCTools.generatePage(1L);
		when(pageRepository.findById(1L)).thenReturn(page);
		when(accessService.hasDeletePermission(1L)).thenReturn(true);
		doThrow(new RuntimeException("Test exception")).when(aclService)
													   .deleteByAclId(page.getAclId());

		try {
			pageService.deleteByUser(1L);
			fail("Should not have been able to delete page with unkown ACL exception");
		} catch (ResponseStatusException e) {
			assertEquals("500 INTERNAL_SERVER_ERROR \"" + VempainMessages.INTERNAL_ERROR + "\"", e.getMessage());
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void deleteByUserUnknownDeleteExceptionFail() throws VempainEntityNotFoundException {
		when(accessService.getValidUserId()).thenReturn(1L);
		Page page = TestUTCTools.generatePage(1L);
		when(pageRepository.findById(1L)).thenReturn(page);
		when(accessService.hasDeletePermission(1L)).thenReturn(true);
		doNothing().when(aclService)
				   .deleteByAclId(1L);
		doThrow(new RuntimeException("Test exception")).when(pageRepository)
													   .delete(page);

		try {
			pageService.deleteByUser(1L);
			fail("Should not have been able to delete page with unkown delete exception");
		} catch (ResponseStatusException e) {
			assertEquals("500 INTERNAL_SERVER_ERROR \"" + VempainMessages.INTERNAL_ERROR + "\"", e.getMessage());
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}
}
