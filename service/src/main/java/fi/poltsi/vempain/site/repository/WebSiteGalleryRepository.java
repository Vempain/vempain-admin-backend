package fi.poltsi.vempain.site.repository;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import fi.poltsi.vempain.site.entity.WebSiteGallery;

@Repository
public interface WebSiteGalleryRepository extends PagingAndSortingRepository<WebSiteGallery, Long>, JpaRepository<WebSiteGallery, Long> {

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "INSERT INTO web_site_gallery_file (gallery_id, file_id, sort_order) VALUES (:galleryId, :fileId, :sortOrder)")
    void saveGalleryFile(@Param("galleryId") long galleryId, @Param("fileId") long fileId, @Param("sortOrder") long sortOrder);

    /**
     * Removes the file relations of a site gallery before they are written again by a republish; runs at once, unlike a derived delete.
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "DELETE FROM web_site_gallery_file WHERE gallery_id = :galleryId")
    void deleteGalleryFiles(@Param("galleryId") long galleryId);

    /**
     * Removes a site file from every site gallery, before the file row itself is removed (the relation has no foreign key to the file).
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "DELETE FROM web_site_gallery_file WHERE file_id = :fileId")
    void deleteGalleryFilesByFileId(@Param("fileId") long fileId);

    /**
     * The site rows of an admin gallery, oldest first; a republish updates the first one in place and removes the others.
     */
    List<WebSiteGallery> findAllByGalleryIdOrderByIdAsc(long galleryId);

    @NonNull
    Page<WebSiteGallery> findAll(@NonNull Pageable pageable);

    @NonNull
    Page<WebSiteGallery> findByShortnameContainingIgnoreCase(String query, @NonNull Pageable pageable);

    @NonNull
    Page<WebSiteGallery> findByDescriptionContainingIgnoreCase(String query, @NonNull Pageable pageable);

    @NonNull
    Page<WebSiteGallery> findByGalleryId(long galleryId, @NonNull Pageable pageable);

    @NonNull
    Page<WebSiteGallery> findByAclId(long aclId, @NonNull Pageable pageable);

    @NonNull
    Page<WebSiteGallery> findByAclIdAndShortnameContainingIgnoreCase(long aclId, String query, @NonNull Pageable pageable);

    @NonNull
    Page<WebSiteGallery> findByAclIdAndDescriptionContainingIgnoreCase(long aclId, String query, @NonNull Pageable pageable);

    @NonNull
    Page<WebSiteGallery> findByAclIdAndGalleryId(long aclId, long galleryId, @NonNull Pageable pageable);
}
