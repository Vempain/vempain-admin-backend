package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.entity.file.Gallery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The gallery search builds its SQL from constants and binds every token as an escaped LIKE pattern (OWASP A05).
 */
class GalleryRepositoryImplUTC {

	private final EntityManager         entityManager = mock(EntityManager.class);
	private final Query                 dataQuery     = mock(Query.class);
	private final Query                 countQuery    = mock(Query.class);
	private final GalleryRepositoryImpl repository    = new GalleryRepositoryImpl(entityManager);

	GalleryRepositoryImplUTC() {
		when(entityManager.createNativeQuery(anyString(), eq(Gallery.class))).thenReturn(dataQuery);
		when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery);
		when(dataQuery.getResultList()).thenReturn(List.of());
		when(countQuery.getSingleResult()).thenReturn(0L);
	}

	@Test
	void tokensAreBoundAsEscapedPatternsWithAnExplicitEscapeClause() {
		repository.searchGalleries("50% \"a_b\" Trip", false, PageRequest.of(0, 10));

		var sql = ArgumentCaptor.forClass(String.class);
		verify(entityManager).createNativeQuery(sql.capture(), eq(Gallery.class));
		assertThat(sql.getValue()).contains("LOWER(g.shortname) LIKE :term0 ESCAPE '\\'")
								  .doesNotContain("50%");
		verify(dataQuery).setParameter("term0", "%50\\%%");
		verify(dataQuery).setParameter("term1", "%a\\_b%");
		verify(dataQuery).setParameter("term2", "%trip%");
		verify(countQuery).setParameter("term0", "%50\\%%");
	}

	@Test
	void caseSensitiveSearchKeepsTheTextButStillEscapesIt() {
		repository.searchGalleriesWithoutFiles("Trip_%", true, PageRequest.of(0, 10));

		var sql = ArgumentCaptor.forClass(String.class);
		verify(entityManager).createNativeQuery(sql.capture(), eq(Gallery.class));
		assertThat(sql.getValue()).contains("g.shortname LIKE :term0 ESCAPE '\\'")
								  .doesNotContain("sf.file_name");
		verify(dataQuery).setParameter("term0", "%Trip\\_\\%%");
	}
}
