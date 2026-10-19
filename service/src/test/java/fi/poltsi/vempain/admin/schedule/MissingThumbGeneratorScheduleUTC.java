package fi.poltsi.vempain.admin.schedule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import fi.poltsi.vempain.admin.entity.file.FileThumb;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.metrics.AdminMetrics;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.admin.service.file.FileThumbService;
import fi.poltsi.vempain.common.api.FileTypeEnum;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Thumbnail rows whose file is missing from the storage are regenerated from the converted site file; rows without a usable site file are only reported.
 */
@ExtendWith(MockitoExtension.class)
class MissingThumbGeneratorScheduleUTC {

    @TempDir
    Path storage;

    @Mock
    private FileService fileService;
    @Mock
    private FileThumbService fileThumbService;

    private MissingThumbGeneratorSchedule schedule;

    @BeforeEach
    void setUp() {
        schedule = new MissingThumbGeneratorSchedule(fileService, fileThumbService, new AdminMetrics(new SimpleMeterRegistry()));
        ReflectionTestUtils.setField(schedule, "siteFileDirectory", storage.toString());
    }

    private static FileThumb thumb(long id, long parentId) {
        return FileThumb.builder()
            .id(id)
            .parentId(parentId)
            .parentType(FileTypeEnum.IMAGE)
            .filepath("thumb/image/2024")
            .filename("a.jpeg")
            .build();
    }

    private static SiteFile siteFile(long id, String fileName) {
        return SiteFile.builder()
            .id(id)
            .fileType(FileTypeEnum.IMAGE)
            .filePath("2024")
            .fileName(fileName)
            .build();
    }

    private void convertedFile(String fileName) throws Exception {
        var directory = Files.createDirectories(storage.resolve("image")
            .resolve("2024"));
        Files.writeString(directory.resolve(fileName), "image bytes");
    }

    @Test
    void nothingHappensWhenSchedulingIsDisabled() {
        ReflectionTestUtils.setField(schedule, "schedulingEnabled", false);

        schedule.findMissingThumbnails();

        verifyNoInteractions(fileService, fileThumbService);
    }

    @Test
    void nothingHappensWithoutThumbRows() {
        when(fileService.findAllFileThumbs()).thenReturn(null);
        schedule.findMissingThumbnails();

        when(fileService.findAllFileThumbs()).thenReturn(List.of());
        schedule.findMissingThumbnails();

        verifyNoInteractions(fileThumbService);
    }

    @Test
    void aMissingThumbFileIsRegeneratedFromTheConvertedSiteFile() throws Exception {
        convertedFile("a.jpg");
        when(fileService.findAllFileThumbs()).thenReturn(List.of(thumb(1L, 10L)));
        when(fileService.findSiteFileById(10L)).thenReturn(Optional.of(siteFile(10L, "a.jpg")));

        schedule.findMissingThumbnails();

        verify(fileThumbService).delete(1L);
        verify(fileThumbService).generateThumbFile(10L);
    }

    @Test
    void anExistingThumbFileIsLeftAlone() throws Exception {
        convertedFile("a.jpg");
        var thumbDirectory = Files.createDirectories(storage.resolve("thumb/image/2024"));
        Files.writeString(thumbDirectory.resolve("a.jpeg"), "thumb bytes");
        when(fileService.findAllFileThumbs()).thenReturn(List.of(thumb(1L, 10L)));
        when(fileService.findSiteFileById(10L)).thenReturn(Optional.of(siteFile(10L, "a.jpg")));

        schedule.findMissingThumbnails();

        verifyNoInteractions(fileThumbService);
    }

    @Test
    void thumbsWithoutAUsableSiteFileAreOnlyReported() throws Exception {
        convertedFile("a.jpg");
        when(fileService.findAllFileThumbs()).thenReturn(List.of(thumb(1L, 10L), thumb(2L, 11L), thumb(3L, 12L)));
        // The site file row is gone
        when(fileService.findSiteFileById(10L)).thenReturn(Optional.empty());
        // The converted file is missing from the storage
        when(fileService.findSiteFileById(11L)).thenReturn(Optional.of(siteFile(11L, "missing.jpg")));
        // The site file has no name: the check path is the directory, which exists, but no thumb can be generated
        when(fileService.findSiteFileById(12L)).thenReturn(Optional.of(siteFile(12L, "")));

        schedule.findMissingThumbnails();

        verify(fileThumbService, never()).delete(anyLong());
        verify(fileThumbService, never()).generateThumbFile(anyLong());
    }
}
