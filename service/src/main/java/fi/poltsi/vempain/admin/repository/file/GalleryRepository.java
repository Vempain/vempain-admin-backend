package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.entity.file.Gallery;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GalleryRepository extends ListPagingAndSortingRepository<Gallery, Long>, CrudRepository<Gallery, Long> {
	Optional<Gallery> findByShortname(String galleryName);
}
