package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.entity.file.SiteFile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteFileRepository extends CrudRepository<SiteFile, Long> {
	Optional<SiteFile> findByFilePathAndFileName(String filePath, String fileName);
}
