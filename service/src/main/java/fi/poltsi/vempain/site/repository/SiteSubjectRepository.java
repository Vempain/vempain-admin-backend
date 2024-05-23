package fi.poltsi.vempain.site.repository;

import fi.poltsi.vempain.site.entity.SiteSubject;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SiteSubjectRepository extends ListPagingAndSortingRepository<SiteSubject, Long>, CrudRepository<SiteSubject, Long> {
	@Transactional
	@Modifying
	@Query(value = "INSERT INTO file_subject (file_id, subject_id) VALUES (:fileId, :subjectId)", nativeQuery = true)
	void saveSiteFileSubject(@Param("fileId") long fileId, @Param("subjectId") long subjectId);

	SiteSubject findBySubject(String subjectName);
}
