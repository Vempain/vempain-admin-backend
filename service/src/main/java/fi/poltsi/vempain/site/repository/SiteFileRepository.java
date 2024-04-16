package fi.poltsi.vempain.site.repository;

import fi.poltsi.vempain.site.entity.SiteFile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteFileRepository extends CrudRepository<SiteFile, Long> {

}
