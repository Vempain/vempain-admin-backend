package fi.poltsi.vempain.admin.repository;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fi.poltsi.vempain.admin.entity.Layout;

@Repository
@Transactional(propagation = Propagation.SUPPORTS)
public interface LayoutRepository extends ListPagingAndSortingRepository<Layout, Long>, ListCrudRepository<Layout, Long> {

    Optional<Layout> findByLayoutName(String layoutName);
}
