package fi.poltsi.vempain.site.repository;

import fi.poltsi.vempain.site.entity.WebSitePage;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface SitePageRepository extends CrudRepository<WebSitePage, Long> {
	WebSitePage findByPath(String path);

	Optional<WebSitePage> findByPageId(long pageId);

	void deletePageById(long id);

	@Transactional
	@Modifying
	@Query(value = "UPDATE page SET cache = NULL", nativeQuery = true)
	void resetCache();

	@Transactional
	@Modifying
	@Query(value = "UPDATE page SET cache = NULL WHERE id = :pageId", nativeQuery = true)
	void resetCacheByPageId(@Param("pageId") long pageId);
}
