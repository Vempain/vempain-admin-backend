package fi.poltsi.vempain.site.rest;

import fi.poltsi.vempain.admin.api.site.WebSiteResourceEnum;
import fi.poltsi.vempain.admin.api.site.request.WebSiteResourcePagedRequest;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import fi.poltsi.vempain.site.entity.WebSiteFile;
import fi.poltsi.vempain.site.entity.WebSiteGallery;
import fi.poltsi.vempain.site.entity.WebSitePage;
import fi.poltsi.vempain.site.repository.WebSiteFileRepository;
import fi.poltsi.vempain.site.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.site.repository.WebSitePageRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
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
									   .filePath("images/alpha.jpg")
									   .mimetype("image/jpeg")
									   .fileType(FileTypeEnum.IMAGE)
									   .aclId(100L)
									   .fileId(1L)
									   .metadata("{}")
									   .build());
		fileRepository.save(WebSiteFile.builder()
									   .filePath("images/beta.jpg")
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
									   .filePath("/home")
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
		var result = mockMvc.perform(post("/admin-management/site/resources")
											 .contentType(MediaType.APPLICATION_JSON)
				                             .content(objectMapper.writeValueAsBytes(resourceRequest(null, null, null, null, null, null, 0, 25)))
				                             .accept(MediaType.APPLICATION_JSON))
							.andExpect(status().isOk())
							.andReturn();
		log.info("MVC Result: {}", result.getResponse()
										 .getContentAsString());
		var response = objectMapper.readValue(result.getResponse()
		                                            .getContentAsString(), new TypeReference<PagedResponse<Object>>() {
		});
		log.info("Response: {}", response);
		assertThat(response.getContent()).hasSizeGreaterThanOrEqualTo(2);
	}

	@Test
	@WithUserDetails(value = "admin", userDetailsServiceBeanName = "userDetailsServiceImpl")
	@DisplayName("Filter by ACL ID and file type")
	void filterByAclAndFileType() throws Exception {
		MvcResult result = mockMvc.perform(post("/admin-management/site/resources")
												   .contentType(MediaType.APPLICATION_JSON)
				                                   .content(objectMapper.writeValueAsBytes(resourceRequest(null, FileTypeEnum.IMAGE, null, 100L, null, null, 0, 25)))
												   .accept(MediaType.APPLICATION_JSON))
								  .andExpect(status().isOk())
								  .andReturn();
		var resp = objectMapper.readValue(result.getResponse()
		                                        .getContentAsByteArray(), new TypeReference<PagedResponse<Object>>() {
		});
		assertThat(resp.getContent()).hasSize(1);
	}

	@Test
	@WithUserDetails(value = "admin", userDetailsServiceBeanName = "userDetailsServiceImpl")
	@DisplayName("List galleries when type=GALLERY")
	void listGalleries() throws Exception {
		MvcResult result = mockMvc.perform(post("/admin-management/site/resources")
												   .contentType(MediaType.APPLICATION_JSON)
				                                   .content(objectMapper.writeValueAsBytes(resourceRequest(WebSiteResourceEnum.GALLERY, null, null, null, null, null, 0, 25)))
												   .accept(MediaType.APPLICATION_JSON))
								  .andExpect(status().isOk())
								  .andReturn();
		var resp = objectMapper.readValue(result.getResponse()
		                                        .getContentAsByteArray(), new TypeReference<PagedResponse<Object>>() {
		});
		assertThat(resp.getContent()).hasSize(1);
	}

	@Test
	@WithUserDetails(value = "admin", userDetailsServiceBeanName = "userDetailsServiceImpl")
	@DisplayName("List pages when type=PAGE with sort=name")
	void listPagesSortedByVirtualName() throws Exception {
		MvcResult result = mockMvc.perform(post("/admin-management/site/resources")
												   .contentType(MediaType.APPLICATION_JSON)
				                                   .content(objectMapper.writeValueAsBytes(resourceRequest(WebSiteResourceEnum.PAGE, null, null, null, "name", "desc", 0, 25)))
												   .accept(MediaType.APPLICATION_JSON))
								  .andExpect(status().isOk())
								  .andReturn();
		var response = objectMapper.readValue(result.getResponse()
		                                            .getContentAsByteArray(), new TypeReference<PagedResponse<Object>>() {
		});
		log.info("Response: {}", response);
		assertThat(response.getContent()).hasSize(1);
	}

	private WebSiteResourcePagedRequest resourceRequest(WebSiteResourceEnum type, FileTypeEnum fileType, String search, Long aclId,
	                                                    String sortBy, String direction, int page, int size) {
		var request = new WebSiteResourcePagedRequest();
		request.setType(type);
		request.setFileType(fileType);
		request.setSearch(search);
		request.setAclId(aclId);
		request.setSortBy(sortBy);
		request.setDirection("desc".equalsIgnoreCase(direction)
		                     ? org.springframework.data.domain.Sort.Direction.DESC
		                     : org.springframework.data.domain.Sort.Direction.ASC);
		request.setPage(page);
		request.setSize(size);
		return request;
	}
}
