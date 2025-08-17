package fi.poltsi.vempain.site.repository;

import fi.poltsi.vempain.site.entity.WebSiteFile;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface WebSiteFileRepository extends CrudRepository<WebSiteFile, Long> {
	@Modifying
	@Transactional
	void deleteByFileId(long fileId);
}
