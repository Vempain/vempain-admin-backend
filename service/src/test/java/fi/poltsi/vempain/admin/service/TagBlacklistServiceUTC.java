package fi.poltsi.vempain.admin.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import fi.poltsi.vempain.admin.api.request.TagBlacklistRequest;
import fi.poltsi.vempain.admin.entity.TagBlacklistEntry;
import fi.poltsi.vempain.admin.repository.TagBlacklistRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagBlacklistServiceUTC {

    @Mock
    private TagBlacklistRepository tagBlacklistRepository;
    @InjectMocks
    private TagBlacklistService tagBlacklistService;

    private static TagBlacklistEntry entry(long id, String name) {
        return TagBlacklistEntry.builder()
            .id(id)
            .tagName(name)
            .createdBy(1L)
            .created(Instant.parse("2026-10-26T12:00:00Z"))
            .build();
    }

    @Test
    void listsTheEntriesInOrder() {
        when(tagBlacklistRepository.findAllByOrderByTagNameAsc()).thenReturn(List.of(entry(2, "family"), entry(1, "private")));

        var responses = tagBlacklistService.findAll();

        assertThat(responses).extracting(r -> r.getTagName())
            .containsExactly("family", "private");
        assertThat(responses.getFirst()
            .getCreatedBy()).isEqualTo(1L);
    }

    @Test
    void addsATrimmedTagOnceAndRefusesBlanksAndDuplicates() {
        when(tagBlacklistRepository.findByTagNameIgnoreCase("private")).thenReturn(Optional.empty());
        when(tagBlacklistRepository.save(any())).thenAnswer(invocation -> {
            var saved = invocation.getArgument(0, TagBlacklistEntry.class);
            saved.setId(9L);
            return saved;
        });

        var response = tagBlacklistService.add(TagBlacklistRequest.builder()
            .tagName("  private ")
            .build(), 7L);

        assertThat(response.getId()).isEqualTo(9L);
        assertThat(response.getTagName()).isEqualTo("private");
        assertThat(response.getCreatedBy()).isEqualTo(7L);
        assertThat(response.getCreated()).isNotNull();
        var saved = ArgumentCaptor.forClass(TagBlacklistEntry.class);
        verify(tagBlacklistRepository).save(saved.capture());
        assertThat(saved.getValue()
            .getTagName()).isEqualTo("private");

        when(tagBlacklistRepository.findByTagNameIgnoreCase("Private")).thenReturn(Optional.of(entry(9, "private")));
        assertThatThrownBy(() -> tagBlacklistService.add(TagBlacklistRequest.builder()
            .tagName("Private")
            .build(), 7L)).isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409");
        assertThatThrownBy(() -> tagBlacklistService.add(TagBlacklistRequest.builder()
            .tagName(" ")
            .build(), 7L)).isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
        assertThatThrownBy(() -> tagBlacklistService.add(null, 7L)).isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }

    @Test
    void deletesAnExistingEntryAndReportsAMissingOne() {
        when(tagBlacklistRepository.findById(3L)).thenReturn(Optional.of(entry(3, "family")));
        when(tagBlacklistRepository.findById(4L)).thenReturn(Optional.empty());

        tagBlacklistService.delete(3L);
        verify(tagBlacklistRepository).delete(entry(3, "family"));

        assertThatThrownBy(() -> tagBlacklistService.delete(4L)).isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");
        verify(tagBlacklistRepository, never()).delete(entry(4, "x"));
    }

    @Test
    void snapshotHoldsEveryTagName() {
        when(tagBlacklistRepository.findAll()).thenReturn(List.of(entry(1, "Private"), entry(2, "family")));

        var snapshot = tagBlacklistService.snapshot();

        assertThat(snapshot.names()).containsExactlyInAnyOrder("private", "family");
        assertThat(snapshot.isBlacklisted("FAMILY")).isTrue();
    }
}
