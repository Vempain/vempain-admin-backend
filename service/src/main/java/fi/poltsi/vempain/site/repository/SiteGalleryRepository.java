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
	void deleteByGalleryId(long galleryId);
}
