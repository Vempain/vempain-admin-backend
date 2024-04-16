package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.entity.Form;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormRepository extends CrudRepository<Form, Long> {
	Form findByFormName(String formName);

	@Query(value = "SELECT CASE WHEN MAX(id) IS NULL THEN 1 ELSE (MAX(id) + 1) END AS next FROM Form")
	long getNextAvailableFormId();

	List<Form> findByLayoutId(long layoutId);
}
