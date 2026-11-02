package fi.poltsi.vempain.admin.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import fi.poltsi.vempain.admin.api.response.TagResponse;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.admin.service.SubjectService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagControllerUTC {

    @Mock
    private SubjectService subjectService;
    @Mock
    private AccessService accessService;
    @InjectMocks
    private TagController controller;

    @Test
    void aSignedInUserGetsTheMatchingTags() {
        var tag = TagResponse.builder()
            .id(3L)
            .tagName("summer")
            .build();
        when(accessService.getValidUserId()).thenReturn(7L);
        when(subjectService.searchTags("sum", 5)).thenReturn(List.of(tag));

        var response = controller.findTags("sum", 5);

        assertEquals(200, response.getStatusCode()
            .value());
        assertSame(tag, response.getBody()
            .getFirst());
        verify(accessService).getValidUserId();
    }

    @Test
    void withoutASessionNothingIsSearched() {
        when(accessService.getValidUserId()).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no session"));

        assertThrows(ResponseStatusException.class, () -> controller.findTags("sum", null));
        verifyNoInteractions(subjectService);
    }
}
