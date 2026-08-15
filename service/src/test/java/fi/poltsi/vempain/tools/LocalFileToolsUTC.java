package fi.poltsi.vempain.tools;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocalFileToolsUTC {
	@Test
	void managesDirectoriesFilesAndExtensions() throws Exception {
		var directory = Files.createTempDirectory("vempain-file-tools");
		var file = directory.resolve("sample.txt");
		Files.writeString(file, "hello");
		LocalFileTools.createAndVerifyDirectory(directory.resolve("nested"));
		assertEquals(5L, LocalFileTools.getFileSize(file));
		assertEquals("sample.sha256", LocalFileTools.setExtension("sample.txt", "sha256"));
		assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
					 LocalFileTools.computeSha256(file.toFile()));
		assertNull(LocalFileTools.computeSha256(directory.resolve("missing")
		                                                 .toFile()));
		LocalFileTools.removeDirectory(directory.toString());
	}
}
