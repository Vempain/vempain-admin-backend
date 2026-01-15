package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.entity.Page;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageRepository extends ListPagingAndSortingRepository<Page, Long>, CrudRepository<Page, Long> {
	// TODO Filter all results through ACL check so that we only return rows to which the user has permissions
	Page findByPagePath(String path);

	Page findById(long id);

	void deletePageById(long id);

	List<Page> findByFormId(long formId);
}
