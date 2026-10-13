package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.entity.file.FileThumb;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.repository.file.FileThumbPageableRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.service.file.FileThumbService;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import fi.poltsi.vempain.common.api.FileTypeEnum;
import fi.poltsi.vempain.tools.ImageTools;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static fi.poltsi.vempain.tools.LocalFileTools.createAndVerifyDirectory;
import static fi.poltsi.vempain.tools.LocalFileTools.removeDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class FileThumbServiceUTC {
	private final String TEST_STORAGE_DIR = "/var/tmp/vempain-test-storage";

	@Mock
	private FileThumbPageableRepository fileThumbPageableRepository;
	@Mock
	private SiteFileRepository          siteFileRepository;
	@Mock
	private ImageTools                  imageTools;

	@InjectMocks
	private FileThumbService fileThumbService;

	@BeforeEach
	void setUp() {
		removeDirectory(TEST_STORAGE_DIR);
		createAndVerifyDirectory(Path.of(TEST_STORAGE_DIR));
		fileThumbService.setSiteFileDirectory(TEST_STORAGE_DIR);
		fileThumbService.setImageFormat("jpeg");
		fileThumbService.setThumbnailSize(250);
	}

	@Test
	void generateThumbFileOk() {
		var siteFileId = 1L;
		var siteFile = TestUTCTools.generateImageSiteFile(siteFileId, TEST_STORAGE_DIR);
		assertNotNull(siteFile);
		when(siteFileRepository.findById(siteFileId)).thenReturn(Optional.of(siteFile));

		var thumbDimension = new Dimension(250, 250);
		when(imageTools.resizeImage(any(Path.class), any(Path.class), anyInt(), anyFloat()))
				.thenReturn(thumbDimension);
		when(imageTools.getImageDimensions(any(Path.class)))
				.thenReturn(thumbDimension);
		var thumbFileName = siteFile.getFileName();
		var thumbFilePath = siteFile.getFilePath();

		var fileThumb = FileThumb.builder()
								 .parentId(siteFileId)
								 .filepath(thumbFilePath)
								 .filename(thumbFileName)
								 .build();

		when(fileThumbPageableRepository.save(any())).thenReturn(fileThumb);

		fileThumbService.generateThumbFile(siteFileId);
		verify(fileThumbPageableRepository, times(1)).save(any(FileThumb.class));
		verify(imageTools, times(1)).resizeImage(any(Path.class), any(Path.class), anyInt(), anyFloat());
		verify(imageTools, times(1)).getImageDimensions(any(Path.class));
	}

	@Test
	void deleteOk() {
		fileThumbService.delete(1L);
		verify(fileThumbPageableRepository, times(1)).deleteById(1L);
	}

	@Test
	void deleteByParentIdOk() {
		fileThumbService.deleteByParentId(5L);
		verify(fileThumbPageableRepository, times(1)).deleteByParentId(5L);
	}


	// ---- invalid ids, existing thumb rows, existing thumb files, unsupported file types ----

	private SiteFile storedSiteFile(long id, FileTypeEnum fileType) throws Exception {
		var directory = Files.createDirectories(Path.of(TEST_STORAGE_DIR, fileType.shortName, "2024"));
		Files.writeString(directory.resolve("a.jpg"), "source bytes");
		return SiteFile.builder()
					   .id(id)
					   .fileType(fileType)
					   .filePath("2024")
					   .fileName("a.jpg")
					   .build();
	}

	@Test
	void anInvalidOrUnknownSiteFileIdGeneratesNothing() {
		fileThumbService.generateThumbFile(0L);
		verifyNoInteractions(siteFileRepository, imageTools, fileThumbPageableRepository);

		when(siteFileRepository.findById(9L)).thenReturn(Optional.empty());
		fileThumbService.generateThumbFile(9L);
		verifyNoInteractions(imageTools, fileThumbPageableRepository);
	}

	@Test
	void anExistingThumbRowIsUpdatedAndAnExistingThumbFileIsReplaced() throws Exception {
		var siteFile = storedSiteFile(1L, FileTypeEnum.IMAGE);
		var existingThumbFile = Files.createDirectories(Path.of(TEST_STORAGE_DIR, "thumb", "image", "2024"))
									 .resolve("a.jpeg");
		Files.writeString(existingThumbFile, "old thumb");
		var existingRow = FileThumb.builder()
								   .id(7L)
								   .parentId(1L)
								   .filepath("old")
								   .filename("old.jpeg")
								   .build();
		when(siteFileRepository.findById(1L)).thenReturn(Optional.of(siteFile));
		when(imageTools.resizeImage(any(Path.class), any(Path.class), anyInt(), anyFloat())).thenReturn(new Dimension(250, 167));
		when(imageTools.getImageDimensions(any(Path.class))).thenReturn(new Dimension(250, 167));
		when(fileThumbPageableRepository.findFileThumbByParentId(1L)).thenReturn(Optional.of(existingRow));

		fileThumbService.generateThumbFile(1L);

		assertFalse(Files.exists(existingThumbFile), "the old thumb file is removed before the new one is generated");
		verify(fileThumbPageableRepository).save(existingRow);
		assertEquals("thumb/image/2024", existingRow.getFilepath());
		assertEquals("a.jpeg", existingRow.getFilename());
		assertEquals(250L, existingRow.getWidth());
		assertEquals(167L, existingRow.getHeight());
		assertNotNull(existingRow.getSha1sum());
	}

	@Test
	void aThumbFileThatCannotBeRemovedIsAnInternalError() throws Exception {
		var siteFile = storedSiteFile(1L, FileTypeEnum.IMAGE);
		// A non-empty directory in the place of the thumb file can not be deleted as a file
		var blocking = Files.createDirectories(Path.of(TEST_STORAGE_DIR, "thumb", "image", "2024", "a.jpeg"));
		Files.writeString(blocking.resolve("child"), "x");
		when(siteFileRepository.findById(1L)).thenReturn(Optional.of(siteFile));

		assertThrows(ResponseStatusException.class, () -> fileThumbService.generateThumbFile(1L));
		verify(imageTools, never()).resizeImage(any(Path.class), any(Path.class), anyInt(), anyFloat());
	}

	@Test
	void unsupportedFileTypesAreNotResizedButStillMeasuredWithTheDefaultFormatAndSize() throws Exception {
		var siteFile = storedSiteFile(2L, FileTypeEnum.VIDEO);
		fileThumbService.setImageFormat(null);
		fileThumbService.setThumbnailSize(0);
		when(siteFileRepository.findById(2L)).thenReturn(Optional.of(siteFile));
		when(imageTools.getImageDimensions(any(Path.class))).thenReturn(new Dimension(0, 0));
		when(fileThumbPageableRepository.findFileThumbByParentId(2L)).thenReturn(Optional.empty());

		fileThumbService.generateThumbFile(2L);

		verify(imageTools, never()).resizeImage(any(Path.class), any(Path.class), anyInt(), anyFloat());
		verify(fileThumbPageableRepository).save(any(FileThumb.class));
	}
}
