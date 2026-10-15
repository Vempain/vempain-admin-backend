package fi.poltsi.vempain.admin.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fi.poltsi.vempain.admin.entity.Component;

@Repository
public interface ComponentRepository extends CrudRepository<Component, Long> {

    Component findByCompName(String compName);
}
