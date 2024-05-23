package fi.poltsi.vempain.site.repository;

import fi.poltsi.vempain.site.entity.SiteFile;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SiteFileRepository extends CrudRepository<SiteFile, Long> {
	@Modifying
	@Transactional
	void deleteByFileId(long fileId);
}
