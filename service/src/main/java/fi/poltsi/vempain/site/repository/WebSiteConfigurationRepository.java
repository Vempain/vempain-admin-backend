package fi.poltsi.vempain.site.repository;

import fi.poltsi.vempain.site.entity.WebSiteConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebSiteConfigurationRepository extends JpaRepository<WebSiteConfiguration, Long> {
}
