package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.entity.file.FileDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileDocumentPageableRepository extends PagingAndSortingRepository<FileDocument, Long>, ListCrudRepository<FileDocument, Long> {
	Page<FileDocument> findByPagesContaining(long filter, PageRequest pageRequest);

	Optional<FileDocument> findFileDocumentByParentId(Long parentId);
}
