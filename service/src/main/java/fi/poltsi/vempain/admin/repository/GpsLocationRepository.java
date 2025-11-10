package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.entity.file.GpsLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GpsLocationRepository extends JpaRepository<GpsLocation, Long> {
}

