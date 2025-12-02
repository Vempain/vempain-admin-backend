package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.entity.Subject;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends PagingAndSortingRepository<Subject, Long>, ListCrudRepository<Subject, Long> {
	Optional<Subject> findSubjectBySubjectName(String subjectName);

	Optional<Subject> findSubjectBySubjectNameDe(String subjectNameDe);

	Optional<Subject> findSubjectBySubjectNameEn(String subjectNameEn);

	Optional<Subject> findSubjectBySubjectNameFi(String subjectNameFi);

	Optional<Subject> findSubjectBySubjectNameSe(String subjectNameSe);

	@Query(value = """
			SELECT s.* FROM subjects s, file_subject fs
			WHERE fs.site_file_id = :siteFileId
			AND   fs.subject_id = s.id
			""", nativeQuery = true)
	List<Subject> getSubjectsByFileId(@Param("siteFileId") Long siteFileId);
}
