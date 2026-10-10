package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.entity.Page;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageRepository extends ListPagingAndSortingRepository<Page, Long>, CrudRepository<Page, Long> {
	// TODO Filter all results through ACL check so that we only return rows to which the user has permissions
	Page findByPagePath(String path);

	Page findById(long id);

	void deletePageById(long id);

	List<Page> findByFormId(long formId);

	/**
	 * Pages whose path starts with the given LIKE pattern; the pattern must come from {@code LikePatterns.prefix} so that request text is
	 * escaped (OWASP A05).
	 */
	@Query("SELECT p FROM Page p WHERE p.pagePath LIKE :pattern ESCAPE '\\' ORDER BY p.pagePath")
	List<Page> findByPagePathPrefix(@Param("pattern") String pattern);
}
