package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.api.response.file.DirectoryNodeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class FileSystemServiceUTC {

	@TempDir
	Path tempDir;

	private FileSystemService fileSystemService;

	@BeforeEach
	void setUp() {
		fileSystemService = new FileSystemService();
		ReflectionTestUtils.setField(fileSystemService, "siteFileDirectory", tempDir.toString());
	}

	// ---- getConvertedDirectoryTree ----

	@Test
	void getConvertedDirectoryTreeEmptyRootOk() {
		List<DirectoryNodeResponse> result = fileSystemService.getConvertedDirectoryTree();

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void getConvertedDirectoryTreeWithSubdirsOk() throws IOException {
		new File(tempDir.toFile(), "images").mkdirs();
		new File(tempDir.toFile(), "documents").mkdirs();

		List<DirectoryNodeResponse> result = fileSystemService.getConvertedDirectoryTree();

		assertNotNull(result);
		assertEquals(2, result.size());
	}

	@Test
	void getConvertedDirectoryTreeExcludesThumbDirOk() throws IOException {
		new File(tempDir.toFile(), "thumb").mkdirs();
		new File(tempDir.toFile(), "images").mkdirs();

		List<DirectoryNodeResponse> result = fileSystemService.getConvertedDirectoryTree();

		assertNotNull(result);
		// "thumb" should be excluded
		assertTrue(result.stream().noneMatch(d -> "thumb".equals(d.getDirectoryName())));
	}

	@Test
	void getConvertedDirectoryTreeWithNestedDirsOk() throws IOException {
		File parent = new File(tempDir.toFile(), "gallery");
		parent.mkdirs();
		new File(parent, "subgallery").mkdirs();

		List<DirectoryNodeResponse> result = fileSystemService.getConvertedDirectoryTree();

		assertNotNull(result);
		assertFalse(result.isEmpty());
		DirectoryNodeResponse gallery = result.stream()
											  .filter(d -> "gallery".equals(d.getDirectoryName()))
											  .findFirst()
											  .orElse(null);
		assertNotNull(gallery);
		assertNotNull(gallery.getChildren());
		assertFalse(gallery.getChildren().isEmpty());
		assertEquals("subgallery", gallery.getChildren().getFirst().getDirectoryName());
	}

	@Test
	void getConvertedDirectoryTreeSkipsFilesOk() throws IOException {
		new File(tempDir.toFile(), "images").mkdirs();
		new File(tempDir.toFile(), "readme.txt").createNewFile();

		List<DirectoryNodeResponse> result = fileSystemService.getConvertedDirectoryTree();

		assertNotNull(result);
		// Only the directory should appear, not the file
		assertEquals(1, result.size());
		assertEquals("images", result.getFirst().getDirectoryName());
	}

	@Test
	void getConvertedDirectoryTreeInvalidPathFail() {
		ReflectionTestUtils.setField(fileSystemService, "siteFileDirectory", "/nonexistent/path/xyz");

		assertThrows(IllegalArgumentException.class, () -> fileSystemService.getConvertedDirectoryTree());
	}
}
