package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.entity.Layout;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(propagation = Propagation.SUPPORTS)
public interface LayoutRepository extends CrudRepository<Layout, Long> {
	Optional<Layout> findByLayoutName(String layoutName);
}
