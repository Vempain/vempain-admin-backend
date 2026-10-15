package fi.poltsi.vempain.admin.repository.file;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fi.poltsi.vempain.admin.entity.file.Gallery;

public interface GalleryRepositoryCustom {

    Page<Gallery> searchGalleries(String searchTerm, boolean caseSensitive, Pageable pageable);

    Page<Gallery> searchGalleriesWithoutFiles(String searchTerm, boolean caseSensitive, Pageable pageable);

    Page<Gallery> searchGalleriesForList(String searchTerm, boolean caseSensitive, Pageable pageable);
}
