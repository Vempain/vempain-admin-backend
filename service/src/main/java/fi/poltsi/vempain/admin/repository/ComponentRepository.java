package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.entity.Component;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComponentRepository extends CrudRepository<Component, Long> {
	Component findByCompName(String compName);
}
