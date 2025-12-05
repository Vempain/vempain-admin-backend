package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.entity.file.Gallery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GalleryRepositoryCustom {
	Page<Gallery> searchGalleries(String searchTerm, boolean caseSensitive, Pageable pageable);
}

