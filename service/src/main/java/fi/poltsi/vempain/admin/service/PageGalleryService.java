package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.api.response.file.GalleryResponse;
import fi.poltsi.vempain.admin.entity.PageGallery;
import fi.poltsi.vempain.admin.service.file.GalleryService;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class PageGalleryService {
	private final EntityManager entityManager;
	private final GalleryService galleryService;

	@Transactional(propagation = Propagation.REQUIRED)
	public void deletePageGallery(Long pageId, Long galleryId, Long sortOrder) {
		var query = entityManager.createNativeQuery("DELETE FROM page_gallery " +
													"WHERE page_id = :pageId " +
													"  AND gallery_id = :galleryId " +
													"  AND sort_order = :sortOrder");
		query.setParameter("pageId", pageId);
		query.setParameter("galleryId", galleryId);
		query.setParameter("sortOrder", sortOrder);
		query.executeUpdate();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void deletePageGalleryByPage(Long pageId) {
		var query = entityManager.createNativeQuery("DELETE FROM page_gallery " +
													"WHERE page_id = :pageId");
		query.setParameter("pageId", pageId);
		query.executeUpdate();
	}

	public List<PageGallery> findPageGalleryByPageId(Long pageId) {
		var query = entityManager.createNativeQuery("SELECT pg.page_id, pg.gallery_id, pg.sort_order " +
													"FROM page_gallery pg " +
													"WHERE pg.page_id = :pageId " +
													"ORDER BY pg.sort_order");
		query.setParameter("pageId", pageId);
		List<Object[]> pgObjects = query.getResultList();
		return mapPageGalleryResults(pgObjects);
	}

	public List<PageGallery> findPageGalleryByGalleryId(Long galleryId) {
		var query = entityManager.createNativeQuery("SELECT pg.page_id, pg.gallery_id, pg.sort_order " +
													"FROM page_gallery pg " +
													"WHERE pg.gallery_id = :galleryId");
		query.setParameter("galleryId", galleryId);
		List<Object[]> pgObjects = query.getResultList();
		return mapPageGalleryResults(pgObjects);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<GalleryResponse> setPageGalleries(long pageId, List<Long> galleryIdList) {
		deletePageGalleryByPage(pageId);
		var galleryResponses = new ArrayList<GalleryResponse>();

		for (int i = 0; i < galleryIdList.size(); i++) {
			addPageGallery(pageId, galleryIdList.get(i), i);
			var galleryResponse = galleryService.findById(galleryIdList.get(i));

			if (galleryResponse != null) {
				galleryResponses.add(galleryResponse);
			}
		}

		return galleryResponses;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void addPageGallery(Long pageId, Long galleryId, int sortOrder) {
		var query = entityManager.createNativeQuery("INSERT INTO page_gallery (page_id, gallery_id, sort_order) " +
													"VALUES (:pageId, :galleryId, :sortOrder)");
		query.setParameter("pageId", pageId);
		query.setParameter("galleryId", galleryId);
		query.setParameter("sortOrder", sortOrder);
		query.executeUpdate();
	}

	private List<PageGallery> mapPageGalleryResults(List<Object[]> pgObjects) {
		var formComponents = new ArrayList<PageGallery>();

		if (!pgObjects.isEmpty()) {
			for (Object[] o : pgObjects) {
				var pageId = (Long) o[0];
				var galleryId = (Long) o[1];
				var sortOrder = (Long) o[2];
				formComponents.add(PageGallery.builder()
											  .pageId(pageId)
											  .galleryId(galleryId)
											  .sortOrder(sortOrder)
											  .build());
			}
		}

		return formComponents;
	}
}
