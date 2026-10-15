package fi.poltsi.vempain.site.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fi.poltsi.vempain.site.entity.WebSiteConfiguration;

@Repository
public interface WebSiteConfigurationRepository extends JpaRepository<WebSiteConfiguration, Long> {
}
