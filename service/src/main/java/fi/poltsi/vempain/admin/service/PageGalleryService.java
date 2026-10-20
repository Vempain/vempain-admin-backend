package fi.poltsi.vempain.admin.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.EntityManager;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fi.poltsi.vempain.admin.api.response.file.GalleryResponse;
import fi.poltsi.vempain.admin.entity.PageGallery;
import fi.poltsi.vempain.admin.repository.PageRepository;
import fi.poltsi.vempain.admin.service.file.GalleryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Service
public class PageGalleryService {

    private final EntityManager entityManager;
    private final GalleryService galleryService;
    private final PageRepository pageRepository;
    private final AccessService accessService;

    @Transactional(propagation = Propagation.REQUIRED)
    public void deletePageGallery(Long pageId, Long galleryId, Long sortOrder) {
        var query = entityManager
            .createNativeQuery("DELETE FROM page_gallery " + "WHERE page_id = :pageId " + "  AND gallery_id = :galleryId " + "  AND sort_order = :sortOrder");
        query.setParameter("pageId", pageId);
        query.setParameter("galleryId", galleryId);
        query.setParameter("sortOrder", sortOrder);
        query.executeUpdate();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deletePageGalleryByPage(Long pageId) {
        var query = entityManager.createNativeQuery("DELETE FROM page_gallery " + "WHERE page_id = :pageId");
        query.setParameter("pageId", pageId);
        query.executeUpdate();
    }

    public List<PageGallery> findPageGalleryByPageId(Long pageId) {
        var query = entityManager.createNativeQuery(
                "SELECT pg.page_id, pg.gallery_id, pg.sort_order " + "FROM page_gallery pg " + "WHERE pg.page_id = :pageId " + "ORDER BY pg.sort_order");
        query.setParameter("pageId", pageId);
        // JPA's native Query API is untyped; the SELECT above yields exactly one Object[] per row
        @SuppressWarnings("unchecked")
        List<Object[]> pgObjects = query.getResultList();
        return mapPageGalleryResults(pgObjects);
    }

    public List<PageGallery> findPageGalleryByGalleryId(Long galleryId) {
        var query = entityManager
            .createNativeQuery("SELECT pg.page_id, pg.gallery_id, pg.sort_order " + "FROM page_gallery pg " + "WHERE pg.gallery_id = :galleryId");
        query.setParameter("galleryId", galleryId);
        // JPA's native Query API is untyped; the SELECT above yields exactly one Object[] per row
        @SuppressWarnings("unchecked")
        List<Object[]> pgObjects = query.getResultList();
        return mapPageGalleryResults(pgObjects);
    }

    /**
     * Replaces the galleries linked to the page with the given ones, in the given order (duplicates and nulls are dropped). The caller needs the modify
     * privilege on the page and on every gallery.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public List<GalleryResponse> setPageGalleries(long pageId, List<Long> requestedGalleryIds) {
        requirePageModify(pageId);
        var galleryIdList = requestedGalleryIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        for (var galleryId : galleryIdList) {
            galleryService.requireModify(galleryId);
        }
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
        requirePageModify(pageId);
        galleryService.requireModify(galleryId);
        var query = entityManager.createNativeQuery("INSERT INTO page_gallery (page_id, gallery_id, sort_order) " + "VALUES (:pageId, :galleryId, :sortOrder)");
        query.setParameter("pageId", pageId);
        query.setParameter("galleryId", galleryId);
        query.setParameter("sortOrder", sortOrder);
        query.executeUpdate();
    }

    private void requirePageModify(long pageId) {
        var page = pageRepository.findById(pageId);
        if (page == null) {
            throw new AccessDeniedException("Page is not accessible");
        }
        if (!accessService.hasModifyPermission(page.getAclId())) {
            throw new AccessDeniedException("User does not have permission to modify page galleries");
        }
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
