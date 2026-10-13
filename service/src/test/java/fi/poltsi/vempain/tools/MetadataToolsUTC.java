package fi.poltsi.vempain.tools;

import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the real exiftool (required by the service, see SetupVerification) against copies of the test images, plus the failure paths
 * of the process handling: a missing executable and an interrupted wait.
 */
class MetadataToolsUTC {
	private static final String EXIFTOOL = "/usr/bin/exiftool";

	@TempDir
	Path tempDir;

	private MetadataTools metadataTools;

	@BeforeEach
	void setUp() {
		assumeTrue(new File(EXIFTOOL).canExecute(), "exiftool is required for this test");
		metadataTools = new MetadataTools();
		ReflectionTestUtils.setField(metadataTools, "exifToolPath", EXIFTOOL);
	}

	@AfterEach
	void clearInterrupt() {
		// Make sure an interrupt set by a test never leaks into the next one
		Thread.interrupted();
	}

	private Path copyTestImage(String name) throws Exception {
		var source = Path.of("src/test/resources/files/Norja-2019-0097.jpeg");
		assumeTrue(Files.exists(source), "test image is required");
		var target = tempDir.resolve(name);
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
		return target;
	}

	@Test
	void metadataOfAnImageIsReturnedAsAJsonArrayWithOneObject() throws Exception {
		var image = copyTestImage("source.jpeg");

		var json = metadataTools.getMetadataAsJSON(image.toFile());

		assertNotNull(json);
		var array = new JSONArray(json);
		assertEquals(1, array.length());
		assertTrue(array.getJSONObject(0)
						.has("File"), json);
	}

	@Test
	void metadataIsCopiedBetweenFilesAndAMissingTargetIsReported() throws Exception {
		var source = copyTestImage("source.jpeg");
		var target = copyTestImage("target.jpeg");

		metadataTools.copyMetadata(source.toFile(), target.toFile());
		// exiftool answers with a non-zero exit code for a target that does not exist; the error is logged, not thrown
		metadataTools.copyMetadata(source.toFile(), tempDir.resolve("missing.jpeg")
														   .toFile());

		var json = metadataTools.getMetadataAsJSON(target.toFile());
		assertNotNull(json);
		assertTrue(json.contains("\"File\""));
	}

	@Test
	void aMissingFileStillAnswersTheToolOutput() throws Exception {
		var json = metadataTools.getMetadataAsJSON(tempDir.resolve("missing.jpeg")
														  .toFile());

		assertNotNull(json);
	}

	@Test
	void aMissingExecutableIsLoggedAndAnswersNull() throws Exception {
		ReflectionTestUtils.setField(metadataTools, "exifToolPath", tempDir.resolve("no-such-exiftool")
																		   .toString());
		var image = copyTestImage("source.jpeg");

		assertNull(metadataTools.getMetadataAsJSON(image.toFile()));
		metadataTools.copyMetadata(image.toFile(), image.toFile());
		assertFalse(Thread.currentThread()
						  .isInterrupted());
	}

	@Test
	void anInterruptedWaitKeepsTheInterruptFlag() throws Exception {
		var image = copyTestImage("source.jpeg");

		Thread.currentThread()
			  .interrupt();
		assertNull(metadataTools.getMetadataAsJSON(image.toFile()));
		assertTrue(Thread.interrupted(), "the interrupt flag is restored after the InterruptedException");

		Thread.currentThread()
			  .interrupt();
		metadataTools.copyMetadata(image.toFile(), image.toFile());
		assertTrue(Thread.interrupted(), "the interrupt flag is restored after the InterruptedException");
	}
}
