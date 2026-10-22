package fi.poltsi.vempain.site.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fi.poltsi.vempain.admin.api.site.WebSiteConfigurationTypeEnum;
import fi.poltsi.vempain.admin.api.site.request.WebSiteConfigurationRequest;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.auth.api.response.UserResponse;
import fi.poltsi.vempain.auth.service.UserService;
import fi.poltsi.vempain.site.entity.WebSiteConfiguration;
import fi.poltsi.vempain.site.repository.WebSiteConfigurationRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSiteConfigurationServiceUTC {

    @Mock
    private WebSiteConfigurationRepository webSiteConfigurationRepository;
    @Mock
    private AccessService accessService;
    @Mock
    private UserService userService;

    @InjectMocks
    private WebSiteConfigurationService webSiteConfigurationService;

    private WebSiteConfiguration buildConfig(long id) {
        return WebSiteConfiguration.builder()
            .id(id)
            .configKey("site.title")
            .configType(WebSiteConfigurationTypeEnum.STRING)
            .configDefault("Default Title")
            .configValue("My Site")
            .build();
    }

    @Test
    void getAllConfigurationsOk() {
        when(webSiteConfigurationRepository.findAll()).thenReturn(List.of(buildConfig(1L)));

        var result = webSiteConfigurationService.getAllConfigurations();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("site.title", result.getFirst()
            .getConfigKey());
    }

    @Test
    void getAllConfigurationsEmptyOk() {
        when(webSiteConfigurationRepository.findAll()).thenReturn(List.of());

        var result = webSiteConfigurationService.getAllConfigurations();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getConfigurationByIdFoundOk() {
        when(webSiteConfigurationRepository.findById(1L)).thenReturn(Optional.of(buildConfig(1L)));

        var result = webSiteConfigurationService.getConfigurationById(1L);

        assertNotNull(result);
        assertEquals("site.title", result.getConfigKey());
        assertEquals("My Site", result.getConfigValue());
    }

    @Test
    void getConfigurationByIdNotFoundReturnsNullOk() {
        when(webSiteConfigurationRepository.findById(99L)).thenReturn(Optional.empty());

        var result = webSiteConfigurationService.getConfigurationById(99L);

        assertNull(result);
    }

    @Test
    void updateConfigurationOk() {
        var request = WebSiteConfigurationRequest.builder()
            .id(1L)
            .configValue("NewValue")
            .build();
        var existing = buildConfig(1L);
        when(webSiteConfigurationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(webSiteConfigurationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accessService.getValidUserId()).thenReturn(7L);
        when(userService.findUserResponseById(7L)).thenReturn(UserResponse.builder()
            .nick("alice")
            .build());

        var result = webSiteConfigurationService.updateConfiguration(request);

        assertNotNull(result);
        assertEquals("NewValue", result.getConfigValue());
        // The update is attributed to the admin user by nick; the seeded row has no creator
        assertEquals("alice", result.getModifier());
        assertNotNull(result.getModified());
        assertNull(result.getCreator());
        assertNull(result.getCreated());
    }

    @Test
    void updateConfigurationNotFoundReturnsNullOk() {
        var request = WebSiteConfigurationRequest.builder()
            .id(99L)
            .configValue("NewValue")
            .build();
        when(webSiteConfigurationRepository.findById(99L)).thenReturn(Optional.empty());

        var result = webSiteConfigurationService.updateConfiguration(request);

        assertNull(result);
    }
}
