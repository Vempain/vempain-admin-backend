package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.api.response.file.GalleryResponse;
import fi.poltsi.vempain.admin.entity.PageGallery;
import fi.poltsi.vempain.admin.repository.PageRepository;
import fi.poltsi.vempain.admin.service.file.GalleryService;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
class PageGalleryServiceUTC {

	@Mock
	private EntityManager entityManager;
	@Mock
	private GalleryService galleryService;
	@Mock
	private Query          query;
	@Mock
	private PageRepository pageRepository;
	@Mock
	private AccessService  accessService;

	@InjectMocks
	private PageGalleryService pageGalleryService;

	@BeforeEach
	void setUp() {
		when(entityManager.createNativeQuery(anyString())).thenReturn(query);
		when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
		when(pageRepository.findById(org.mockito.ArgumentMatchers.anyLong())).thenReturn(TestUTCTools.generatePage(1L));
		when(accessService.hasModifyPermission(org.mockito.ArgumentMatchers.anyLong())).thenReturn(true);
		org.mockito.Mockito.doNothing()
		                   .when(galleryService)
		                   .requireModify(org.mockito.ArgumentMatchers.anyLong());
	}

	// ---- deletePageGallery ----

	@Test
	void deletePageGalleryOk() {
		when(query.executeUpdate()).thenReturn(1);

		pageGalleryService.deletePageGallery(1L, 2L, 0L);

		verify(query).executeUpdate();
	}

	@Test
	void deletePageGalleryNoMatchOk() {
		when(query.executeUpdate()).thenReturn(0);

		pageGalleryService.deletePageGallery(99L, 99L, 99L);

		verify(query).executeUpdate();
	}

	// ---- deletePageGalleryByPage ----

	@Test
	void deletePageGalleryByPageOk() {
		when(query.executeUpdate()).thenReturn(1);

		pageGalleryService.deletePageGalleryByPage(1L);

		verify(query).executeUpdate();
	}

	// ---- findPageGalleryByPageId ----

	@Test
	void findPageGalleryByPageIdOk() {
		Object[] row = {10L, 20L, 0L};
		when(query.getResultList()).thenReturn(Collections.singletonList(row));

		List<PageGallery> result = pageGalleryService.findPageGalleryByPageId(10L);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(10L, result.getFirst().getPageId());
		assertEquals(20L, result.getFirst().getGalleryId());
		assertEquals(0L, result.getFirst().getSortOrder());
	}

	@Test
	void findPageGalleryByPageIdEmptyOk() {
		when(query.getResultList()).thenReturn(Collections.emptyList());

		List<PageGallery> result = pageGalleryService.findPageGalleryByPageId(99L);

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	// ---- findPageGalleryByGalleryId ----

	@Test
	void findPageGalleryByGalleryIdOk() {
		Object[] row = {5L, 7L, 1L};
		when(query.getResultList()).thenReturn(Collections.singletonList(row));

		List<PageGallery> result = pageGalleryService.findPageGalleryByGalleryId(7L);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(5L, result.getFirst().getPageId());
		assertEquals(7L, result.getFirst().getGalleryId());
	}

	@Test
	void findPageGalleryByGalleryIdEmptyOk() {
		when(query.getResultList()).thenReturn(Collections.emptyList());

		List<PageGallery> result = pageGalleryService.findPageGalleryByGalleryId(99L);

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	// ---- setPageGalleries ----

	@Test
	void setPageGalleriesOk() {
		when(query.executeUpdate()).thenReturn(1);
		GalleryResponse galleryResponse = GalleryResponse.builder().id(20L).build();
		when(galleryService.findById(20L)).thenReturn(galleryResponse);

		List<GalleryResponse> responses = pageGalleryService.setPageGalleries(1L, List.of(20L));

		assertNotNull(responses);
		assertEquals(1, responses.size());
		assertEquals(20L, responses.getFirst().getId());
	}

	@Test
	void setPageGalleriesGalleryNotFoundOk() {
		when(query.executeUpdate()).thenReturn(1);
		when(galleryService.findById(99L)).thenReturn(null);

		List<GalleryResponse> responses = pageGalleryService.setPageGalleries(1L, List.of(99L));

		assertNotNull(responses);
		assertTrue(responses.isEmpty());
	}

	@Test
	void setPageGalleriesEmptyListOk() {
		when(query.executeUpdate()).thenReturn(0);

		List<GalleryResponse> responses = pageGalleryService.setPageGalleries(1L, Collections.emptyList());

		assertNotNull(responses);
		assertTrue(responses.isEmpty());
	}

	// ---- addPageGallery ----

	@Test
	void addPageGalleryOk() {
		when(query.executeUpdate()).thenReturn(1);

		pageGalleryService.addPageGallery(1L, 2L, 0);

		verify(query).executeUpdate();
	}
}
