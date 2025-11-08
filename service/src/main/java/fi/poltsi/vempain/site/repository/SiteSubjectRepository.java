package fi.poltsi.vempain.site.repository;

import fi.poltsi.vempain.site.entity.WebSiteSubject;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SiteSubjectRepository extends ListPagingAndSortingRepository<WebSiteSubject, Long>, CrudRepository<WebSiteSubject, Long> {
	@Transactional
	@Modifying
	@Query(value = "INSERT INTO web_site_file_subject (file_id, subject_id) VALUES (:fileId, :subjectId)", nativeQuery = true)
	void saveSiteFileSubject(@Param("fileId") long fileId, @Param("subjectId") long subjectId);

	WebSiteSubject findBySubjectName(String subjectName);
}
