package fi.poltsi.vempain.admin.schedule;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import fi.poltsi.vempain.admin.entity.file.FileThumb;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.metrics.AdminMetrics;
import fi.poltsi.vempain.admin.service.file.FileService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Thumbnail rows that share a path and name: the ones without a site file are removed, the ones with a site file are only counted (their removal is still a
 * TODO in the schedule).
 */
@ExtendWith(MockitoExtension.class)
class DuplicateFileRemovalScheduleUTC {

    @Mock
    private FileService fileService;

    @Spy
    private AdminMetrics adminMetrics = new AdminMetrics(new SimpleMeterRegistry());
    @InjectMocks
    private DuplicateFileRemovalSchedule schedule;

    private static FileThumb thumb(long id, long parentId) {
        return FileThumb.builder()
            .id(id)
            .parentId(parentId)
            .filepath("thumb/image/2024")
            .filename("a.jpeg")
            .build();
    }

    @Test
    void nothingHappensWhenSchedulingIsDisabled() {
        ReflectionTestUtils.setField(schedule, "schedulingEnabled", false);

        schedule.removeDuplicateImages();

        verifyNoInteractions(fileService);
    }

    @Test
    void nothingIsRemovedWithoutDuplicates() {
        when(fileService.getDuplicateThumbFiles()).thenReturn(List.of());

        schedule.removeDuplicateImages();

        verify(fileService, never()).deleteFileThumb(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void orphanedDuplicatesAreRemovedAndTheOnesWithASiteFileAreKept() {
        var reported = thumb(1L, 10L);
        var orphan = thumb(2L, 99L);
        var keptFirst = thumb(1L, 10L);
        var keptSecond = thumb(3L, 11L);
        when(fileService.getDuplicateThumbFiles()).thenReturn(List.of(reported));
        when(fileService.findAllFileThumbsByFilepathFilename("thumb/image/2024", "a.jpeg")).thenReturn(List.of(keptFirst, orphan, keptSecond));
        when(fileService.findSiteFileById(10L)).thenReturn(Optional.of(SiteFile.builder()
            .id(10L)
            .build()));
        when(fileService.findSiteFileById(11L)).thenReturn(Optional.of(SiteFile.builder()
            .id(11L)
            .build()));
        when(fileService.findSiteFileById(99L)).thenReturn(Optional.empty());

        schedule.removeDuplicateImages();

        // Thumb rows sharing a path and name are equal, so the removed row is identified by its id
        var removed = org.mockito.ArgumentCaptor.forClass(FileThumb.class);
        verify(fileService).deleteFileThumb(removed.capture());
        assertEquals(2L, removed.getValue()
            .getId());
    }
}
