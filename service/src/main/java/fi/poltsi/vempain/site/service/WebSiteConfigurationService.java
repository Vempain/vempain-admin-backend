package fi.poltsi.vempain.site.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fi.poltsi.vempain.admin.api.site.request.WebSiteConfigurationRequest;
import fi.poltsi.vempain.admin.api.site.response.WebSiteConfigurationResponse;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.auth.service.UserService;
import fi.poltsi.vempain.site.entity.WebSiteConfiguration;
import fi.poltsi.vempain.site.repository.WebSiteConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSiteConfigurationService {

    private final WebSiteConfigurationRepository webSiteConfigurationRepository;
    private final AccessService accessService;
    private final UserService userService;

    public List<WebSiteConfigurationResponse> getAllConfigurations() {
        return webSiteConfigurationRepository.findAll()
            .stream()
            .map(WebSiteConfiguration::toResponse)
            .toList();
    }

    public WebSiteConfigurationResponse getConfigurationById(long id) {
        return webSiteConfigurationRepository.findById(id)
            .map(WebSiteConfiguration::toResponse)
            .orElse(null);
    }

    @Transactional
    public WebSiteConfigurationResponse updateConfiguration(WebSiteConfigurationRequest request) {
        var existingConfigOpt = webSiteConfigurationRepository.findById(request.getId());
        if (existingConfigOpt.isEmpty()) {
            log.warn("No configuration found with id {}", request.getId());
            return null;
        }

        var existingConfig = existingConfigOpt.get();
        existingConfig.setConfigValue(request.getConfigValue());
        // The audit trail of the site row names the admin user by nick, the website has no access to the admin user base
        existingConfig.setModifier(userService.findUserResponseById(accessService.getValidUserId())
            .getNick());
        existingConfig.setModified(Instant.now());
        var newConfiguration = webSiteConfigurationRepository.save(existingConfig);

        return newConfiguration.toResponse();
    }
}
