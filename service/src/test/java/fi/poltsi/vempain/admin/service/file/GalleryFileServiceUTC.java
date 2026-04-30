package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.entity.file.GalleryFile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GalleryFileServiceUTC {

	@Mock
	private EntityManager    entityManager;
	@Mock
	private FileThumbService fileThumbService;
	@Mock
	private Query            query;

	@InjectMocks
	private GalleryFileService galleryFileService;

	@BeforeEach
	void setUp() {
		when(entityManager.createNativeQuery(anyString())).thenReturn(query);
		when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
	}

	// ---- deleteAllGalleryFiles ----

	@Test
	void deleteAllGalleryFilesOk() {
		doNothing().when(entityManager).joinTransaction();
		when(query.executeUpdate()).thenReturn(5);

		galleryFileService.deleteAllGalleryFiles();

		verify(query).executeUpdate();
	}

	// ---- deleteGalleryFile ----

	@Test
	void deleteGalleryFileOk() {
		when(query.executeUpdate()).thenReturn(1);

		galleryFileService.deleteGalleryFile(1L, 2L, 0L);

		verify(query).executeUpdate();
	}

	@Test
	void deleteGalleryFileNoMatchOk() {
		when(query.executeUpdate()).thenReturn(0);

		galleryFileService.deleteGalleryFile(99L, 99L, 99L);

		verify(query).executeUpdate();
	}

	// ---- findGalleryFileByGalleryId ----

	@Test
	void findGalleryFileByGalleryIdOk() {
		Object[] row = {1L, 10L, 0L};
		when(query.getResultList()).thenReturn(List.of(row));

		List<GalleryFile> result = galleryFileService.findGalleryFileByGalleryId(1L);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(1L, result.getFirst().getGalleryId());
		assertEquals(10L, result.getFirst().getSiteFileId());
		assertEquals(0L, result.getFirst().getSortOrder());
	}

	@Test
	void findGalleryFileByGalleryIdEmptyOk() {
		when(query.getResultList()).thenReturn(Collections.emptyList());

		List<GalleryFile> result = galleryFileService.findGalleryFileByGalleryId(99L);

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void findGalleryFileByGalleryIdMultipleOk() {
		Object[] row1 = {1L, 10L, 0L};
		Object[] row2 = {1L, 11L, 1L};
		when(query.getResultList()).thenReturn(List.of(row1, row2));

		List<GalleryFile> result = galleryFileService.findGalleryFileByGalleryId(1L);

		assertNotNull(result);
		assertEquals(2, result.size());
	}

	// ---- addGalleryFile ----

	@Test
	void addGalleryFileOk() {
		when(query.executeUpdate()).thenReturn(1);
		doNothing().when(fileThumbService).generateThumbFile(10L);

		galleryFileService.addGalleryFile(1L, 10L, 0L);

		verify(query).executeUpdate();
		verify(fileThumbService).generateThumbFile(10L);
	}

	// ---- addGalleryFiles ----

	@Test
	void addGalleryFilesOk() {
		when(query.executeUpdate()).thenReturn(1);
		doNothing().when(fileThumbService).generateThumbFile(org.mockito.ArgumentMatchers.anyLong());

		galleryFileService.addGalleryFiles(1L, new long[]{10L, 11L, 12L});

		verify(fileThumbService, org.mockito.Mockito.times(3)).generateThumbFile(org.mockito.ArgumentMatchers.anyLong());
	}

	@Test
	void addGalleryFilesEmptyArrayOk() {
		galleryFileService.addGalleryFiles(1L, new long[]{});
		// No interactions expected
	}

	// ---- deleteGalleryFilesByGalleryId ----

	@Test
	void deleteGalleryFilesByGalleryIdOk() {
		when(query.executeUpdate()).thenReturn(3);

		galleryFileService.deleteGalleryFilesByGalleryId(1L);

		verify(query).executeUpdate();
	}

	// ---- updateGalleryFiles ----

	@Test
	void updateGalleryFilesOk() {
		when(query.executeUpdate()).thenReturn(1);
		doNothing().when(fileThumbService).generateThumbFile(org.mockito.ArgumentMatchers.anyLong());

		galleryFileService.updateGalleryFiles(1L, new long[]{20L});

		// deleteGalleryFilesByGalleryId + addGalleryFile
		verify(query, org.mockito.Mockito.atLeast(1)).executeUpdate();
		verify(fileThumbService).generateThumbFile(20L);
	}
}
