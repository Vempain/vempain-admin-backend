package fi.poltsi.vempain.admin.repository;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import fi.poltsi.vempain.admin.entity.DataEntity;

@Repository
public interface DataRepository extends ListPagingAndSortingRepository<DataEntity, Long>, ListCrudRepository<DataEntity, Long> {

    Optional<DataEntity> findByIdentifier(String identifier);

    boolean existsByIdentifier(String identifier);
}
