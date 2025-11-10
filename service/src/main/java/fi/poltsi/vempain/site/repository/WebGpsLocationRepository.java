package fi.poltsi.vempain.site.repository;

import fi.poltsi.vempain.site.entity.WebGpsLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebGpsLocationRepository extends JpaRepository<WebGpsLocation, Long> {
}

