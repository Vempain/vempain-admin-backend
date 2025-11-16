package fi.poltsi.vempain.site.repository;

import fi.poltsi.vempain.site.entity.WebSitePage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface SitePageRepository extends PagingAndSortingRepository<WebSitePage, Long>, JpaRepository<WebSitePage, Long> {
	WebSitePage findByPath(String path);

	Optional<WebSitePage> findByPageId(long pageId);

	void deletePageById(long id);

	@Transactional
	@Modifying
	@Query(value = "UPDATE web_site_page SET cache = NULL", nativeQuery = true)
	void resetCache();

	@Transactional
	@Modifying
	@Query(value = "UPDATE web_site_page SET cache = NULL WHERE id = :pageId", nativeQuery = true)
	void resetCacheByPageId(@Param("pageId") long pageId);

	Page<WebSitePage> findAll(Pageable pageable);

	@NonNull
	Page<WebSitePage> findByTitleContainingIgnoreCase(String query, @NonNull Pageable pageable);

	@NonNull
	Page<WebSitePage> findByPathContainingIgnoreCase(String path, @NonNull Pageable pageable);

	@NonNull
	Page<WebSitePage> findByAclId(long aclId, @NonNull Pageable pageable);

	@NonNull
	Page<WebSitePage> findByAclIdAndTitleContainingIgnoreCase(long aclId, String query, @NonNull Pageable pageable);

	@NonNull
	Page<WebSitePage> findByAclIdAndPathContainingIgnoreCase(long aclId, String path, @NonNull Pageable pageable);
}
