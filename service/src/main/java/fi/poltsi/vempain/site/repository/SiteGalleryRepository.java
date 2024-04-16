package fi.poltsi.vempain.site.repository;

import fi.poltsi.vempain.site.entity.SiteGallery;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SiteGalleryRepository extends ListPagingAndSortingRepository<SiteGallery, Long>, CrudRepository<SiteGallery, Long> {
	@Query(nativeQuery = true, value = "INSERT INTO gallery_file (gallery_id, file_id, sort_order) VALUES (:galleryId, :fileId, :sortOrder)")
	void saveGalleryFile(@Param("galleryId") long galleryId, @Param("fileId") long fileId, @Param("sortOrder") long sortOrder);

	@Modifying
	@Query(value = "INSERT INTO gallery (id, description, shortname, creator, created, modifier, modified) " +
				   "VALUES (:#{#siteGallery.id}, :#{#siteGallery.description}, :#{#siteGallery.shortname}, " +
				   ":#{#siteGallery.creator}, :#{#siteGallery.created}, :#{#siteGallery.modifier}, " +
				   ":#{#siteGallery.modified}) " +
				   "ON DUPLICATE KEY UPDATE " +
				   "description = VALUES(description), shortname = VALUES(shortname), " +
				   "creator = VALUES(creator), created = VALUES(created), modifier = VALUES(modifier), " +
				   "modified = VALUES(modified)",
		   nativeQuery = true)
	@Transactional
	void saveGallery(@Param("siteGallery") SiteGallery siteGallery);
}
