package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.entity.DataEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DataRepository extends CrudRepository<DataEntity, Long> {
	Optional<DataEntity> findByIdentifier(String identifier);

	boolean existsByIdentifier(String identifier);
}
