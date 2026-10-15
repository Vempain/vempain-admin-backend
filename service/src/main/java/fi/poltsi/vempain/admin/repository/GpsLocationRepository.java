package fi.poltsi.vempain.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fi.poltsi.vempain.admin.entity.file.GpsLocation;

@Repository
public interface GpsLocationRepository extends JpaRepository<GpsLocation, Long> {
}
