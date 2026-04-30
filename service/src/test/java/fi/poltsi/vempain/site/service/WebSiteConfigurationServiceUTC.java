package fi.poltsi.vempain.site.service;

import fi.poltsi.vempain.admin.api.site.WebSiteConfigurationTypeEnum;
import fi.poltsi.vempain.admin.api.site.request.WebSiteConfigurationRequest;
import fi.poltsi.vempain.site.entity.WebSiteConfiguration;
import fi.poltsi.vempain.site.repository.WebSiteConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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
		assertEquals("site.title", result.getFirst().getConfigKey());
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

		var result = webSiteConfigurationService.updateConfiguration(request);

		assertNotNull(result);
		assertEquals("NewValue", result.getConfigValue());
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
