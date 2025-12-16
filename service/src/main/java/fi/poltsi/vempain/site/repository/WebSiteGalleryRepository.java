package fi.poltsi.vempain.site.repository;

import fi.poltsi.vempain.site.entity.WebSiteGallery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface WebSiteGalleryRepository extends PagingAndSortingRepository<WebSiteGallery, Long>, JpaRepository<WebSiteGallery, Long> {
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "INSERT INTO web_site_gallery_file (gallery_id, file_id, sort_order) VALUES (:galleryId, :fileId, :sortOrder)")
	void saveGalleryFile(@Param("galleryId") long galleryId, @Param("fileId") long fileId, @Param("sortOrder") long sortOrder);

	@Modifying
	@Transactional
	void deleteByGalleryId(long galleryId);

	Page<WebSiteGallery> findAll(Pageable pageable);

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
