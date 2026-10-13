package fi.poltsi.vempain.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ImageToolsUTC {
	@TempDir
	Path tempDir;

	private MetadataTools metadataTools;
	private ImageTools    imageTools;

	@BeforeEach
	void setUp() {
		metadataTools = mock(MetadataTools.class);
		imageTools = new ImageTools();
		ReflectionTestUtils.setField(imageTools, "metadataTools", metadataTools);
	}

	private Path image(String name, int width, int height) throws Exception {
		var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		var graphics = image.createGraphics();
		graphics.setColor(Color.ORANGE);
		graphics.fillRect(0, 0, width, height);
		graphics.dispose();
		var path = tempDir.resolve(name);
		assertTrue(ImageIO.write(image, "jpeg", path.toFile()));
		return path;
	}

	@Test
	void landscapeImagesAreScaledSoThatTheHeightBecomesTheMinimumSize() throws Exception {
		var source = image("landscape.jpg", 400, 200);
		var target = tempDir.resolve("landscape-thumb.jpg");

		var dimension = imageTools.resizeImage(source, target, 100, 0.8f);

		assertEquals(new Dimension(200, 100), dimension);
		assertEquals(new Dimension(200, 100), imageTools.getImageDimensions(target));
		verify(metadataTools).copyMetadata(source.toFile(), target.toFile());
	}

	@Test
	void portraitImagesAreScaledSoThatTheWidthBecomesTheMinimumSize() throws Exception {
		var source = image("portrait.jpg", 200, 400);
		var target = tempDir.resolve("portrait-thumb.jpg");

		var dimension = imageTools.resizeImage(source, target, 100, 0.8f);

		assertEquals(new Dimension(100, 200), dimension);
		assertEquals(new Dimension(100, 200), imageTools.getImageDimensions(target));
	}

	@Test
	void imagesSmallerThanTheMinimumKeepTheirSize() throws Exception {
		var source = image("small.jpg", 30, 20);
		var target = tempDir.resolve("small-thumb.jpg");

		var dimension = imageTools.resizeImage(source, target, 100, 0.8f);

		assertEquals(new Dimension(30, 20), dimension);
		assertEquals(new Dimension(30, 20), imageTools.getImageDimensions(target));
	}

	@Test
	void anUnwritableTargetIsAnInternalError() throws Exception {
		var source = image("source.jpg", 40, 40);
		var target = tempDir.resolve("no-such-directory")
							.resolve("thumb.jpg");

		var thrown = assertThrows(ResponseStatusException.class, () -> imageTools.resizeImage(source, target, 100, 0.8f));

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, thrown.getStatusCode());
		verify(metadataTools, never()).copyMetadata(any(), any());
	}

	@Test
	void dimensionsOfAMissingFileAreAnInternalError() {
		var thrown = assertThrows(ResponseStatusException.class, () -> imageTools.getImageDimensions(tempDir.resolve("missing.jpg")));

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, thrown.getStatusCode());
	}

	@Test
	void dimensionsOfADirectoryAreAnInternalError() throws Exception {
		var directory = Files.createDirectory(tempDir.resolve("a-directory"));

		assertThrows(ResponseStatusException.class, () -> imageTools.getImageDimensions(directory));
	}

	@Test
	void dimensionsOfANonImageAreAnInternalError() throws Exception {
		var text = tempDir.resolve("notes.txt");
		Files.writeString(text, "plain text, no image reader applies");

		assertThrows(ResponseStatusException.class, () -> imageTools.getImageDimensions(text));
	}

	@Test
	void dimensionsOfATruncatedImageAreAnInternalError() throws Exception {
		var truncated = tempDir.resolve("truncated.jpg");
		// A JPEG start-of-image marker and nothing else: the reader is found but cannot read the header
		Files.write(truncated, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});

		assertThrows(ResponseStatusException.class, () -> imageTools.getImageDimensions(truncated));
	}
}
