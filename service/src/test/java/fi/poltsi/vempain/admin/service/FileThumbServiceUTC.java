package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.entity.file.FileThumb;
import fi.poltsi.vempain.admin.repository.file.FileThumbPageableRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.service.file.FileThumbService;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import fi.poltsi.vempain.tools.ImageTools;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.*;
import java.nio.file.Path;
import java.util.Optional;

import static fi.poltsi.vempain.tools.LocalFileTools.createAndVerifyDirectory;
import static fi.poltsi.vempain.tools.LocalFileTools.removeDirectory;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
		fileThumbService.setConvertedDirectory(TEST_STORAGE_DIR);
		fileThumbService.setImageFormat("jpeg");
		fileThumbService.setThumbnailSize(250);
	}

	@Test
	void generateThumbFileOk() {
		var siteFileId = 1L;
		var siteFile = TestUTCTools.generateSiteFile(siteFileId, TEST_STORAGE_DIR);
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

}
