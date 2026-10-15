package fi.poltsi.vempain.admin.repository.file;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import fi.poltsi.vempain.admin.entity.file.Gallery;

@Repository
public interface GalleryRepository extends ListPagingAndSortingRepository<Gallery, Long>, CrudRepository<Gallery, Long>, GalleryRepositoryCustom {

    Optional<Gallery> findByShortname(String galleryName);

    @Query("SELECT g.id FROM Gallery g")
    List<Long> getAllGalleryIds();
}
