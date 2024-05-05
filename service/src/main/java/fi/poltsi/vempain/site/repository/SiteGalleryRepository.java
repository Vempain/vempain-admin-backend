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
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "INSERT INTO gallery_file (gallery_id, file_id, sort_order) VALUES (:galleryId, :fileId, :sortOrder)")
	void saveGalleryFile(@Param("galleryId") long galleryId, @Param("fileId") long fileId, @Param("sortOrder") long sortOrder);

	@Modifying
	@Transactional
	@Query(value = "INSERT INTO gallery (id, description, shortname, creator, created, modifier, modified) " +
				   "VALUES (:#{#siteGallery.id}, :#{#siteGallery.description}, :#{#siteGallery.shortname}, " +
				   ":#{#siteGallery.creator}, :#{#siteGallery.created}, :#{#siteGallery.modifier}, " +
				   ":#{#siteGallery.modified}) " +
				   "ON CONFLICT (id) DO UPDATE " +  // Specify the conflict resolution on id column
				   "SET description = :#{#siteGallery.description}, " +
				   "shortname = :#{#siteGallery.shortname}, " +
				   "creator = :#{#siteGallery.creator}, " +
				   "created = :#{#siteGallery.created}, " +
				   "modifier = :#{#siteGallery.modifier}, " +
				   "modified = :#{#siteGallery.modified}",
		   nativeQuery = true)
	void saveGallery(@Param("siteGallery") SiteGallery siteGallery);
}
