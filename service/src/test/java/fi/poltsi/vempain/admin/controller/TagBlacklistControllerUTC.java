package fi.poltsi.vempain.admin.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fi.poltsi.vempain.admin.api.request.TagBlacklistRequest;
import fi.poltsi.vempain.admin.api.response.TagBlacklistResponse;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.admin.service.TagBlacklistService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagBlacklistControllerUTC {

    @Mock
    private TagBlacklistService tagBlacklistService;
    @Mock
    private AccessService accessService;
    @InjectMocks
    private TagBlacklistController controller;

    @Test
    void everyOperationRequiresAnAdministratorAndDelegatesToTheService() {
        var entry = TagBlacklistResponse.builder()
            .id(3L)
            .tagName("private")
            .build();
        var request = TagBlacklistRequest.builder()
            .tagName("private")
            .build();
        when(tagBlacklistService.findAll()).thenReturn(List.of(entry));
        when(accessService.getValidUserId()).thenReturn(7L);
        when(tagBlacklistService.add(request, 7L)).thenReturn(entry);

        assertSame(entry, controller.getBlacklist()
            .getBody()
            .getFirst());
        var created = controller.addToBlacklist(request);
        assertEquals(201, created.getStatusCode()
            .value());
        assertSame(entry, created.getBody());
        assertEquals(204, controller.removeFromBlacklist(3L)
            .getStatusCode()
            .value());

        verify(accessService, times(3)).checkAdminAccess();
        verify(tagBlacklistService).delete(3L);
    }
}
