package fi.poltsi.vempain.site.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.poltsi.vempain.admin.api.site.WebSiteResourceEnum;
import fi.poltsi.vempain.admin.api.site.response.WebSiteResourcePageResponse;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import fi.poltsi.vempain.site.entity.WebSiteFile;
import fi.poltsi.vempain.site.entity.WebSiteGallery;
import fi.poltsi.vempain.site.entity.WebSitePage;
import fi.poltsi.vempain.site.repository.WebSiteFileRepository;
import fi.poltsi.vempain.site.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.site.repository.WebSitePageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"vempain.test=false"})
class WebSiteManagementRTC {
	@Autowired
	private MockMvc               mockMvc;
	@Autowired
	private WebSiteFileRepository    fileRepository;
	@Autowired
	private WebSiteGalleryRepository galleryRepository;
	@Autowired
	private WebSitePageRepository    pageRepository;
	@Autowired
	private ObjectMapper          objectMapper;

	@BeforeEach
	void initData() {
		fileRepository.deleteAll();
		galleryRepository.deleteAll();
		pageRepository.deleteAll();
		// rely on Flyway-seeded admin user

		fileRepository.save(WebSiteFile.builder()
									   .path("images/alpha.jpg")
									   .mimetype("image/jpeg")
									   .fileType(FileTypeEnum.IMAGE)
									   .aclId(100L)
									   .fileId(1L)
									   .metadata("{}")
									   .build());
		fileRepository.save(WebSiteFile.builder()
									   .path("images/beta.jpg")
									   .mimetype("image/jpeg")
									   .fileType(FileTypeEnum.IMAGE)
									   .aclId(101L)
									   .fileId(2L)
									   .metadata("{}")
									   .build());

		galleryRepository.save(WebSiteGallery.builder()
											 .shortname("Winter-2025")
											 .description("Snowy days")
											 .aclId(200L)
											 .galleryId(10L)
											 .creator(1L)
											 .created(Instant.now())
											 .build());

		pageRepository.save(WebSitePage.builder()
									   .title("Home Page")
									   .path("/home")
									   .secure(false)
									   .indexList(false)
									   .aclId(300L)
									   .pageId(55L)
									   .body("Welcome")
									   .header("Header")
									   .creator("admin")
									   .created(Instant.now())
									   .build());
	}

	@Test
	@WithUserDetails(value = "admin", userDetailsServiceBeanName = "userDetailsServiceImpl")
	@DisplayName("List site files default paging")
	void listSiteFilesDefault() throws Exception {
		var result = mockMvc.perform(get("/admin-management/site/resources").accept(MediaType.APPLICATION_JSON))
							.andExpect(status().isOk())
							.andReturn();
		var resp = objectMapper.readValue(result.getResponse()
												.getContentAsByteArray(), new TypeReference<WebSiteResourcePageResponse>() {
		});
		assertThat(resp.getItems()).hasSizeGreaterThanOrEqualTo(2);
	}

	@Test
	@WithUserDetails(value = "admin", userDetailsServiceBeanName = "userDetailsServiceImpl")
	@DisplayName("Filter by ACL ID and file type")
	void filterByAclAndFileType() throws Exception {
		MvcResult result = mockMvc.perform(get("/admin-management/site/resources")
												   .param("acl_id", "100")
												   .param("file_type", FileTypeEnum.IMAGE.name())
												   .accept(MediaType.APPLICATION_JSON))
								  .andExpect(status().isOk())
								  .andReturn();
		var resp = objectMapper.readValue(result.getResponse()
												.getContentAsByteArray(), new TypeReference<WebSiteResourcePageResponse>() {
		});
		assertThat(resp.getItems()).hasSize(1);
	}

	@Test
	@WithUserDetails(value = "admin", userDetailsServiceBeanName = "userDetailsServiceImpl")
	@DisplayName("List galleries when type=GALLERY")
	void listGalleries() throws Exception {
		MvcResult result = mockMvc.perform(get("/admin-management/site/resources")
												   .param("type", WebSiteResourceEnum.GALLERY.name())
												   .accept(MediaType.APPLICATION_JSON))
								  .andExpect(status().isOk())
								  .andReturn();
		var resp = objectMapper.readValue(result.getResponse()
												.getContentAsByteArray(), new TypeReference<WebSiteResourcePageResponse>() {
		});
		assertThat(resp.getItems()).hasSize(1);
	}

	@Test
	@WithUserDetails(value = "admin", userDetailsServiceBeanName = "userDetailsServiceImpl")
	@DisplayName("List pages when type=PAGE with sort=name")
	void listPagesSortedByVirtualName() throws Exception {
		MvcResult result = mockMvc.perform(get("/admin-management/site/resources")
												   .param("type", WebSiteResourceEnum.PAGE.name())
												   .param("sort", "name")
												   .param("direction", "desc")
												   .accept(MediaType.APPLICATION_JSON))
								  .andExpect(status().isOk())
								  .andReturn();
		var resp = objectMapper.readValue(result.getResponse()
												.getContentAsByteArray(), new TypeReference<WebSiteResourcePageResponse>() {
		});
		assertThat(resp.getItems()).hasSize(1);
	}
}
