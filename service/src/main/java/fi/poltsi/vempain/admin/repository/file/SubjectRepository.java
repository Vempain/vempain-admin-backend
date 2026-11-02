package fi.poltsi.vempain.admin.repository.file;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fi.poltsi.vempain.admin.entity.Subject;

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

    /**
     * Subjects whose name contains the given LIKE pattern, case-insensitively and alphabetically; the pattern must come from
     * {@code LikePatterns.containsIgnoreCase} so that request text is escaped (OWASP A05).
     */
    @Query("SELECT s FROM Subject s WHERE LOWER(s.subjectName) LIKE :pattern ESCAPE '\\' ORDER BY s.subjectName")
    List<Subject> searchBySubjectName(@Param("pattern") String pattern, Pageable pageable);

    @Query(value = """
            SELECT s.* FROM subjects s, page_subject ps
            WHERE ps.page_id = :pageId
            AND   ps.subject_id = s.id
            ORDER BY s.subject
            """, nativeQuery = true)
    List<Subject> getSubjectsByPageId(@Param("pageId") Long pageId);
}
