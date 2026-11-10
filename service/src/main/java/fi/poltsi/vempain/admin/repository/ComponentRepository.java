package fi.poltsi.vempain.admin.repository;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import fi.poltsi.vempain.admin.entity.Component;

@Repository
public interface ComponentRepository extends ListPagingAndSortingRepository<Component, Long>, ListCrudRepository<Component, Long> {

    Component findByCompName(String compName);
}
