package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.request.LayoutRequest;
import fi.poltsi.vempain.admin.api.response.LayoutResponse;
import fi.poltsi.vempain.admin.entity.Layout;
import fi.poltsi.vempain.admin.exception.ProcessingFailedException;
import fi.poltsi.vempain.admin.exception.VempainLayoutException;
import fi.poltsi.vempain.admin.repository.LayoutRepository;
import fi.poltsi.vempain.admin.tools.MockRepositoryTools;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import fi.poltsi.vempain.auth.entity.AbstractVempainEntity;
import fi.poltsi.vempain.auth.exception.VempainAbstractException;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
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
class LayoutServiceUTC {
    private final static long count = 10;

    @Mock
    private LayoutRepository layoutRepository;
    @Mock
	private AclService       aclService;
    @Mock
	private AccessService    accessService;

	@InjectMocks
    private LayoutService layoutService;

    @Test
    void findAllOk() {
        MockRepositoryTools.layoutRepositoryFindAllOk(layoutRepository, count);

        Iterable<Layout> layouts = layoutService.findAll();
        assertNotNull(layouts);
        assertEquals(count, StreamSupport.stream(layouts.spliterator(), false).count());
    }

    @Test
    void findAllNoneFoundOk() {
        MockRepositoryTools.layoutRepositoryFindAllOk(layoutRepository, 0);

        Iterable<Layout> layouts = layoutService.findAll();
        assertNotNull(layouts);
        assertEquals(0, StreamSupport.stream(layouts.spliterator(), false).count());
    }

    @Test
    void findAllByUserOk() {
        MockRepositoryTools.layoutRepositoryFindAllOk(layoutRepository, count);
        when(accessService.hasReadPermission(1L)).thenReturn(true);

        List<LayoutResponse> layoutResponses = layoutService.findAllByUser();
        assertNotNull(layoutResponses);
        assertEquals(count, layoutResponses.size());
    }

    @Test
    void findAllByUserNoneFoundOk() {
        MockRepositoryTools.layoutRepositoryFindAllOk(layoutRepository, 0);

        List<LayoutResponse> layoutResponses = layoutService.findAllByUser();
        assertNotNull(layoutResponses);
        assertEquals(0, layoutResponses.size());
    }

    @Test
    void findAllByUserNoPermissionOk() {
        MockRepositoryTools.layoutRepositoryFindAllOk(layoutRepository, count);
        when(accessService.hasReadPermission(1L)).thenReturn(false);

        List<LayoutResponse> layoutResponses = layoutService.findAllByUser();
        assertNotNull(layoutResponses);
        assertEquals(0, layoutResponses.size());
    }

    @Test
    void findByIdOk() {
        MockRepositoryTools.layoutRepositoryfindByIdOk(layoutRepository, count);

        try {
            Layout layout = layoutService.findById(count);
            assertNotNull(layout);
            assertEquals(count, layout.getId());
        } catch (VempainEntityNotFoundException e) {
            fail("Should have received a layout as response");
        }
    }

    @Test
    void findByIdNoneFoundOk() {
        when(layoutRepository.findById(count)).thenReturn(Optional.empty());

        try {
            layoutService.findById(count);
            fail("Should have thrown an exception when the ID does not exist");
        } catch (VempainEntityNotFoundException e) {
            assertEquals("Failed to find layout by ID", e.getMessage());
        }
    }

    @Test
    void findByIdByUserOk() {
        MockRepositoryTools.layoutRepositoryfindByIdOk(layoutRepository, count);
        when(accessService.getValidUserId()).thenReturn(1L);
        when(accessService.hasReadPermission(anyLong())).thenReturn(true);

        try {
            Layout layout = layoutService.findByIdByUser(count);
            assertNotNull(layout);
            assertEquals(count, layout.getId());
        } catch (VempainEntityNotFoundException e) {
            fail("Should have received a layout as response");
        }
    }

    @Test
    void findByIdByUserNoSessionFail() {
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Test exception")).when(accessService).getValidUserId();

        try {
            layoutService.findByIdByUser(count);
            fail("Searching by id and user without session should have triggered an exception");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
            assertEquals("401 UNAUTHORIZED \"Test exception\"", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void findByIdByUserNoPermissionFail() {
        MockRepositoryTools.layoutRepositoryfindByIdOk(layoutRepository, count);
        when(accessService.hasReadPermission(anyLong())).thenReturn(false);

        try {
            layoutService.findByIdByUser(count);
            fail("Searching by id and user without permission should have triggered an exception");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
            assertEquals("401 UNAUTHORIZED \"" + VempainMessages.UNAUTHORIZED_ACCESS + "\"", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void findByNameOk() {
        String layoutName = "test layout";
        MockRepositoryTools.layoutRepositoryFindByNameOk(layoutRepository, layoutName);

        try {
            Layout layout = layoutService.findByLayoutName(layoutName);
            assertNotNull(layout);
            assertEquals(layoutName, layout.getLayoutName());
        } catch (VempainLayoutException e) {
            fail("Should have received a layout as response");
        }
    }

    @Test
    void findByNameNoneFound() {
        String layoutName = "test layout";
        when(layoutRepository.findByLayoutName(layoutName)).thenReturn(Optional.empty());

        try {
            layoutService.findByLayoutName(layoutName);
            fail("Should have thrown an exception when the ID does not exist");
        } catch (VempainLayoutException e) {
            assertEquals(VempainMessages.OBJECT_NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void findLayoutResponseByLayoutNameByUserOk() {
        String layoutName = "Test layout";
        Layout layout = TestUTCTools.generateLayout(1L);
        layout.setLayoutName(layoutName);

        when(accessService.getValidUserId()).thenReturn(1L);

        Optional<Layout> optionalLayout = Optional.of(layout);
        when(layoutRepository.findByLayoutName(layoutName)).thenReturn(optionalLayout);
        when(accessService.hasReadPermission(anyLong())).thenReturn(true);

        try {
            LayoutResponse layoutResponse = layoutService.findLayoutResponseByLayoutNameByUser(layoutName);
            assertNotNull(layoutResponse);
        } catch (Exception e) {
            fail("Should not have received any exception: " + e);
        }
    }

    @Test
    void findLayoutResponseByLayoutNameByUserNoSessionFail() {
        String layoutName = "Test layout";
        Layout layout = TestUTCTools.generateLayout(1L);
        layout.setLayoutName(layoutName);

        when(accessService.getValidUserId()).thenReturn(1L);

        Optional<Layout> optionalLayout = Optional.of(layout);
        when(layoutRepository.findByLayoutName(layoutName)).thenReturn(optionalLayout);
        when(accessService.hasReadPermission(anyLong())).thenReturn(false);

        try {
            layoutService.findLayoutResponseByLayoutNameByUser(layoutName);
            fail("Searching by name and user without session should have triggered an exception");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
            assertEquals("401 UNAUTHORIZED \"" + VempainMessages.UNAUTHORIZED_ACCESS + "\"", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void findLayoutResponseByLayoutNameByUserNoPermissionFail() {
        String layoutName = "Test layout";
        Layout layout = TestUTCTools.generateLayout(1L);
        layout.setLayoutName(layoutName);

        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Test exception")).when(accessService).getValidUserId();

        try {
            layoutService.findLayoutResponseByLayoutNameByUser("");
            fail("Searching by name and user without session should have triggered an exception");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
            assertEquals("401 UNAUTHORIZED \"Test exception\"", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void saveOk() {
        Layout layout = MockRepositoryTools.makeLayout(1L, "Test layout");

        when(layoutRepository.save(layout)).thenReturn(layout);

        try {
            layoutService.save(layout);
        } catch (VempainLayoutException | VempainAbstractException e) {
            fail("Should have not received an exception when saving a complete layout");
        }
    }

    @Test
    void saveNullLayoutFail() {
        try {
            layoutService.save(null);
            fail("Saving a null layout should have failed");
        } catch (VempainLayoutException e) {
            assertEquals("Layout is null", e.getMessage());
        } catch (Exception e) {
            fail("We should not have gotten any other exception: " + e);
        }
    }

    @Test
    void saveNullLayoutNameFail() {
        Layout layout = MockRepositoryTools.makeLayout(1L, null);

        try {
            layoutService.save(layout);
            fail("Saving a layout with null name should have failed");
        } catch (VempainLayoutException e) {
            assertEquals("Layout name is null or blank", e.getMessage());
        } catch (Exception e) {
            fail("We should not have gotten any other exception: " + e);
        }
    }

    @Test
    void saveEmptyLayoutNameFail() {
        Layout layout = MockRepositoryTools.makeLayout(1L, "");
        layout.setLayoutName("");

        try {
            layoutService.save(layout);
            fail("Saving a layout with empty name should have failed");
        } catch (VempainLayoutException e) {
            assertEquals("Layout name is null or blank", e.getMessage());
        } catch (Exception e) {
            fail("We should not have gotten any other exception: " + e);
        }
    }

    @Test
    void saveNullStructureFail() {
        Layout layout = MockRepositoryTools.makeLayout(1L, "test layout");
        layout.setStructure(null);

        try {
            layoutService.save(layout);
            fail("Saving a layout with null structure should have failed");
        } catch (VempainLayoutException e) {
            assertEquals("Layout structure is null or blank", e.getMessage());
        } catch (Exception e) {
            fail("We should not have gotten any other exception: " + e);
        }
    }

    @Test
    void saveBlankStructureFail() {
        Layout layout = MockRepositoryTools.makeLayout(1L, "test layout");
        layout.setStructure("  ");

        try {
            layoutService.save(layout);
            fail("Saving a layout with empty structure should have failed");
        } catch (VempainLayoutException e) {
            assertEquals("Layout structure is null or blank", e.getMessage());
        } catch (Exception e) {
            fail("We should not have gotten any other exception: " + e);
        }
    }

    @Test
    void saveInvalidAclFail() throws VempainAbstractException {
        Layout layout = MockRepositoryTools.makeLayout(1L, "Test layout");
        layout.setAclId(-1L);

		doThrow(new VempainAbstractException("ACL ID is invalid"))
				.when(aclService).validateAbstractData(any(AbstractVempainEntity.class));

        try {
            layoutService.save(layout);
            fail("Saving a layout with invalid ACL ID should have failed");
        } catch (VempainAbstractException e) {
            assertEquals("ACL ID is invalid", e.getMessage());
        } catch (Exception e) {
            fail("We should not have gotten any other exception: " + e);
        }
    }

    @Test
    void saveNegativeCreatorFail() throws VempainAbstractException {
        Layout layout = MockRepositoryTools.makeLayout(1L, "Test layout");
        layout.setCreator(-1L);

		doThrow(new VempainAbstractException("Creator is missing or invalid"))
				.when(aclService).validateAbstractData(any(AbstractVempainEntity.class));

        try {
            layoutService.save(layout);
            fail("Saving a layout with invalid creator ID should have failed");
        } catch (VempainAbstractException e) {
            assertEquals("Creator is missing or invalid", e.getMessage());
        } catch (Exception e) {
            fail("We should not have gotten any other exception: " + e);
        }
    }

    @Test
    void saveNullCreatedFail() throws VempainAbstractException {
        Layout layout = MockRepositoryTools.makeLayout(1L, "Test layout");
        layout.setCreated(null);

		doThrow(new VempainAbstractException("Created datetime is missing"))
				.when(aclService).validateAbstractData(any(AbstractVempainEntity.class));

        try {
            layoutService.save(layout);
            fail("Saving a layout without a created timestamp should have failed");
        } catch (VempainAbstractException e) {
            assertEquals("Created datetime is missing", e.getMessage());
        } catch (Exception e) {
            fail("We should not have gotten any other exception: " + e);
        }
    }

    @Test
    void saveNullModifierFail() throws VempainAbstractException {
        Layout layout = MockRepositoryTools.makeLayout(1L, "Test layout");
        layout.setModifier(null);

		doThrow(new VempainAbstractException("Modifier is missing while modified is set"))
				.when(aclService).validateAbstractData(any(AbstractVempainEntity.class));

        try {
            layoutService.save(layout);
            fail("Saving a layout with modified set but no modifier should have failed");
        } catch (VempainAbstractException e) {
            assertEquals("Modifier is missing while modified is set", e.getMessage());
        } catch (Exception e) {
            fail("We should not have gotten any other exception: " + e);
        }
    }

    @Test
    void saveNullModifiedFail() throws VempainAbstractException {
        Layout layout = MockRepositoryTools.makeLayout(1L, "Test layout");
        layout.setModified(null);

		doThrow(new VempainAbstractException("Modified datetime is missing while modifier is set"))
				.when(aclService).validateAbstractData(any(AbstractVempainEntity.class));

        try {
            layoutService.save(layout);
            fail("Saving a layout with modifier set but no modified should have failed");
        } catch (VempainAbstractException e) {
            assertEquals("Modified datetime is missing while modifier is set", e.getMessage());
        } catch (Exception e) {
            fail("We should not have gotten any other exception: " + e);
        }
    }

    @Test
    void saveNullModifiedBeforeCreatedFail() throws VempainAbstractException {
        Layout layout = MockRepositoryTools.makeLayout(1L, "Test layout");
        layout.setModified(Instant.now().minus(1, ChronoUnit.HOURS));
        layout.setCreated(Instant.now());

		doThrow(new VempainAbstractException("Created datetime is more recent than modified"))
				.when(aclService).validateAbstractData(any(AbstractVempainEntity.class));

        try {
            layoutService.save(layout);
            fail("Saving a layout with created timestamp after modified should have failed");
        } catch (VempainAbstractException e) {
            assertEquals("Created datetime is more recent than modified", e.getMessage());
        } catch (Exception e) {
            fail("We should not have gotten any other exception: " + e);
        }
    }

    @Test
    void saveLayoutRequestByUserOk() throws VempainAclException {
        String layoutName = "Test layout";
        Layout layout = MockRepositoryTools.makeLayout(1L, layoutName);
        LayoutRequest layoutRequest = TestUTCTools.generateLayoutRequest(layout);

        when(accessService.getValidUserId()).thenReturn(1L);
        when(layoutRepository.findByLayoutName(layoutName)).thenReturn(Optional.empty());
        when(aclService.getNextAclId()).thenReturn(1L);
        doNothing().when(aclService).saveAclRequests(1L, layoutRequest.getAcls());
        when(layoutRepository.save(any(Layout.class))).thenReturn(layout);

        try {
            Layout returnLayout = layoutService.saveLayoutRequestByUser(layoutRequest);
            assertNotNull(returnLayout);
        } catch (Exception e) {
            fail("Should have not received an exception when saving a layout request");
        }
    }

    @Test
    void saveLayoutRequestByUserNoSessionFail() {
        String layoutName = "Test layout";
        Layout layout = MockRepositoryTools.makeLayout(1L, layoutName);
        LayoutRequest layoutRequest = TestUTCTools.generateLayoutRequest(layout);

        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Test exception")).when(accessService).getValidUserId();

        try {
            layoutService.saveLayoutRequestByUser(layoutRequest);
            fail("Saving a layout request without a user session should have triggered an exception");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
            assertEquals("401 UNAUTHORIZED \"Test exception\"", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void saveLayoutRequestByUserLayoutNameExistsFail() {
        String layoutName = "Test layout";
        Layout layout = MockRepositoryTools.makeLayout(1L, layoutName);
        LayoutRequest layoutRequest = TestUTCTools.generateLayoutRequest(layout);

        when(accessService.getValidUserId()).thenReturn(1L);
        Optional<Layout> optionalLayout = Optional.of(layout);
        when(layoutRepository.findByLayoutName(layoutName)).thenReturn(optionalLayout);

        try {
            layoutService.saveLayoutRequestByUser(layoutRequest);
            fail("Saving a layout request with existing layout name should have triggered an exception");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
            assertEquals("409 CONFLICT \"" + VempainMessages.OBJECT_NAME_ALREADY_EXISTS + "\"", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void saveLayoutRequestByUserAclExceptionFail() throws VempainAclException {
        String layoutName = "Test layout";
        Layout layout = MockRepositoryTools.makeLayout(1L, layoutName);
        LayoutRequest layoutRequest = TestUTCTools.generateLayoutRequest(layout);

        when(accessService.getValidUserId()).thenReturn(1L);
        when(layoutRepository.findByLayoutName(layoutName)).thenReturn(Optional.empty());
        when(aclService.getNextAclId()).thenReturn(1L);
        doThrow(new VempainAclException("Test exception")).when(aclService).saveAclRequests(1L, layoutRequest.getAcls());

        try {
            layoutService.saveLayoutRequestByUser(layoutRequest);
            fail("Saving a layout request while triggering AclException should have triggered a ResponseStatusException");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
            assertEquals("500 INTERNAL_SERVER_ERROR \"" + VempainMessages.INTERNAL_ERROR + "\"", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void saveLayoutRequestByUserLayoutExceptionFail() throws VempainAclException {
        String layoutName = "Test layout";
        Layout layout = MockRepositoryTools.makeLayout(1L, layoutName);
        layout.setStructure("  ");
        LayoutRequest layoutRequest = TestUTCTools.generateLayoutRequest(layout);

        when(accessService.getValidUserId()).thenReturn(1L);
        when(layoutRepository.findByLayoutName(layoutName)).thenReturn(Optional.empty());
        when(aclService.getNextAclId()).thenReturn(1L);
        doNothing().when(aclService).saveAclRequests(1L, layoutRequest.getAcls());

        try {
            layoutService.saveLayoutRequestByUser(layoutRequest);
            fail("Saving a layout request with null layout name should have triggered an exception");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
            assertEquals("500 INTERNAL_SERVER_ERROR \"" + VempainMessages.INTERNAL_ERROR + "\"", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void updateByUserOk() throws VempainAclException {
        String layoutName = "Test layout";
        Layout layout = MockRepositoryTools.makeLayout(1L, layoutName);
        LayoutRequest layoutRequest = TestUTCTools.generateLayoutRequest(layout);

        when(accessService.getValidUserId()).thenReturn(1L);
        Optional<Layout> optionalLayout = Optional.of(layout);
        when(layoutRepository.findById(1L)).thenReturn(optionalLayout);
        when(accessService.hasModifyPermission(1L)).thenReturn(true);
        doNothing().when(aclService).updateFromRequestList(layoutRequest.getAcls());
        when(layoutRepository.save(layout)).thenReturn(layout);

        try {
            Layout response = layoutService.updateByUser(layoutRequest);
            assertNotNull(response);
        } catch (Exception e) {
            fail("Updating layout should have worked, instead got exception " + e.getMessage());
        }
    }

    @Test
    void updateByUserNoSessionFail() {
        String layoutName = "Test layout";
        Layout layout = MockRepositoryTools.makeLayout(1L, layoutName);
        LayoutRequest layoutRequest = TestUTCTools.generateLayoutRequest(layout);

        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Test exception")).when(accessService).getValidUserId();

        try {
            layoutService.updateByUser(layoutRequest);
            fail("updating layout without session should have triggered an exception");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
            assertEquals("401 UNAUTHORIZED \"Test exception\"", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void updateByUserNoLayoutFail() {
        String layoutName = "Test layout";
        Layout layout = MockRepositoryTools.makeLayout(1L, layoutName);
        LayoutRequest layoutRequest = TestUTCTools.generateLayoutRequest(layout);

        when(accessService.getValidUserId()).thenReturn(1L);

        Optional<Layout> emptyOptional = Optional.empty();
        when(layoutRepository.findById(1L)).thenReturn(emptyOptional);

        try {
            layoutService.updateByUser(layoutRequest);
            fail("updating non-existing layout should have triggered an exception");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
            assertEquals("404 NOT_FOUND \"" + VempainMessages.OBJECT_NOT_FOUND + "\"", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void updateByUserNoPermissionFail() {
        String layoutName = "Test layout";
        Layout layout = MockRepositoryTools.makeLayout(1L, layoutName);
        LayoutRequest layoutRequest = TestUTCTools.generateLayoutRequest(layout);

        when(accessService.getValidUserId()).thenReturn(1L);
        Optional<Layout> optionalLayout = Optional.of(layout);
        when(layoutRepository.findById(1L)).thenReturn(optionalLayout);
        when(accessService.hasModifyPermission(1L)).thenReturn(false);

        try {
            layoutService.updateByUser(layoutRequest);
            fail("Updating layout without permission should have triggered an exception");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
            assertEquals("401 UNAUTHORIZED \"" + VempainMessages.UNAUTHORIZED_ACCESS + "\"", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void updateByUserAclSaveFail() throws VempainAclException {
        String layoutName = "Test layout";
        Layout layout = MockRepositoryTools.makeLayout(1L, layoutName);
        LayoutRequest layoutRequest = TestUTCTools.generateLayoutRequest(layout);

        when(accessService.getValidUserId()).thenReturn(1L);
        Optional<Layout> optionalLayout = Optional.of(layout);
        when(layoutRepository.findById(1L)).thenReturn(optionalLayout);
        when(accessService.hasModifyPermission(1L)).thenReturn(true);
        doThrow(new VempainAclException("Test exception")).when(aclService).updateFromRequestList(layoutRequest.getAcls());

        try {
            layoutService.updateByUser(layoutRequest);
            fail("Updating layout and failing with ACL storing should have triggered an exception");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
            assertEquals("500 INTERNAL_SERVER_ERROR \"" + VempainMessages.INTERNAL_ERROR + "\"", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void deleteOk() throws VempainEntityNotFoundException {
        Layout layout = MockRepositoryTools.makeLayout(1L, "Test layout");
        Optional<Layout> optionalLayout = Optional.of(layout);
        when(layoutRepository.findById(1L)).thenReturn(optionalLayout);
        doNothing().when(aclService).deleteByAclId(1L);
        doNothing().when(layoutRepository).delete(layout);

        try {
            layoutService.delete(1L);
        } catch (Exception e) {
            fail("Deleting layout should have worked, instead got exception " + e.getMessage());
        }
    }

    @Test
    void deleteAclExceptionOk() throws VempainEntityNotFoundException {
        Layout layout = MockRepositoryTools.makeLayout(1L, "Test layout");
        Optional<Layout> optionalLayout = Optional.of(layout);
        when(layoutRepository.findById(1L)).thenReturn(optionalLayout);
        doThrow(new VempainEntityNotFoundException("No Acls found with id: 1", "acl")).when(aclService).deleteByAclId(1L);
        doNothing().when(layoutRepository).delete(layout);

        try {
            layoutService.delete(1L);
        } catch (Exception e) {
            fail("We should have not have received an exception even when no ACLs were found");
        }
    }

    @Test
    void deleteRuntimeExceptionFail() throws VempainEntityNotFoundException {
        Layout layout = MockRepositoryTools.makeLayout(1L, "Test layout");
        Optional<Layout> optionalLayout = Optional.of(layout);
        when(layoutRepository.findById(1L)).thenReturn(optionalLayout);
        doThrow(new RuntimeException("Test exception")).when(aclService).deleteByAclId(1L);

        try {
            layoutService.delete(1L);
            fail("Deleting layout with ACL runtime exception should have failed");
        } catch (VempainAclException e) {
            assertEquals("Failed to remove ACL", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void deleteFailProcessingException() throws VempainEntityNotFoundException {
        Layout layout = MockRepositoryTools.makeLayout(1L, "Test layout");
        Optional<Layout> optionalLayout = Optional.of(layout);
        when(layoutRepository.findById(1L)).thenReturn(optionalLayout);
        doThrow(new RuntimeException("Test fail")).when(layoutRepository).delete(layout);
        doNothing().when(aclService).deleteByAclId(1L);

        try {
            layoutService.delete(1L);
            fail("Deleting layout with ACL exception should have failed");
        } catch (ProcessingFailedException e) {
            assertEquals("Failed to delete layout", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void deleteByUserOk() throws VempainEntityNotFoundException {
        Layout layout = TestUTCTools.generateLayout(1L);
        when(accessService.getValidUserId()).thenReturn(1L);
        when(accessService.hasDeletePermission(anyLong())).thenReturn(true);
        Optional<Layout> optionalLayout = Optional.of(layout);
        when(layoutRepository.findById(1L)).thenReturn(optionalLayout);
        doNothing().when(aclService).deleteByAclId(1L);

        try {
            layoutService.deleteByUser(1L);
        } catch (Exception e) {
            fail("We should have not have received an exception when call is ok: " + e);
        }
    }

    @Test
    void deleteByUserNoSessionFail() {
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Test exception")).when(accessService).getValidUserId();

        try {
            layoutService.deleteByUser(1L);
            fail("Deleting by id without session should have triggered an exception");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
            assertEquals("401 UNAUTHORIZED \"Test exception\"", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void deleteByUserNoPermissionFail() {
        Layout layout = TestUTCTools.generateLayout(1L);
        when(accessService.getValidUserId()).thenReturn(1L);
        Optional<Layout> optionalLayout = Optional.of(layout);
        when(layoutRepository.findById(layout.getId())).thenReturn(optionalLayout);
        when(accessService.hasDeletePermission(anyLong())).thenReturn(false);

        try {
            layoutService.deleteByUser(1L);
            fail("Deleting by id and user without permission should have triggered an exception");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
            assertEquals("401 UNAUTHORIZED \"" + VempainMessages.UNAUTHORIZED_ACCESS + "\"", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void deleteByUserNoAclFail() throws VempainEntityNotFoundException {
        Layout layout = TestUTCTools.generateLayout(1L);
        when(accessService.getValidUserId()).thenReturn(1L);
        Optional<Layout> optionalLayout = Optional.of(layout);
        when(layoutRepository.findById(layout.getId())).thenReturn(optionalLayout);
        when(accessService.hasDeletePermission(anyLong())).thenReturn(true);
        doThrow(new RuntimeException("Test fail")).when(aclService).deleteByAclId(1L);

        try {
            layoutService.deleteByUser(1L);
            fail("Deleting by id and user without permission should have triggered an exception");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
            assertEquals("500 INTERNAL_SERVER_ERROR \"" + VempainMessages.INTERNAL_ERROR + "\"", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }
}
