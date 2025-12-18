package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.api.PublishResultEnum;
import fi.poltsi.vempain.admin.api.response.PublishResponse;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.admin.service.file.GalleryFileService;
import fi.poltsi.vempain.auth.service.UserService;
import fi.poltsi.vempain.site.repository.WebGpsLocationRepository;
import fi.poltsi.vempain.site.repository.WebSiteFileRepository;
import fi.poltsi.vempain.site.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.site.repository.WebSitePageRepository;
import fi.poltsi.vempain.site.service.WebSiteResourceService;
import fi.poltsi.vempain.site.service.WebSiteSubjectService;
import fi.poltsi.vempain.tools.JschClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishServiceUTC {
	@Mock
	private SiteFileRepository       siteFileRepository;
	@Mock
	private WebSitePageRepository    webSitePageRepository;
	@Mock
	private WebSiteGalleryRepository webSiteGalleryRepository;
	@Mock
	private WebSiteFileRepository    webSiteFileRepository;
	@Mock
	private WebGpsLocationRepository webGpsLocationRepository;
	@Mock
	private PageService              pageService;
	@Mock
	private FormService              formService;
	@Mock
	private ComponentService         componentService;
	@Mock
	private FileService              fileService;
	@Mock
	private LayoutService            layoutService;
	@Mock
	private UserService              userService;
	@Mock
	private SubjectService           subjectService;
	@Mock
	private GalleryFileService    galleryFileService;
	@Mock
	private WebSiteSubjectService webSiteSubjectService;
	@Mock
	private PageGalleryService    pageGalleryService;
	@Mock
	private JschClient               jschClient;
	@Mock
	private WebSiteResourceService   webSiteResourceService;
	@Mock
	private AccessService            accessService;

	@InjectMocks
	private PublishService publishService;

	private PublishService publishServiceSpy;

	@BeforeEach
	void setupSpy() {
		publishServiceSpy = Mockito.spy(publishService);
	}

	@Test
	void publishSelectedGalleriesPublishesAllowedOnes() throws Exception {
		var gallery = Gallery.builder()
							 .id(1L)
							 .aclId(101L)
							 .build();
		when(fileService.findGalleryById(1L)).thenReturn(gallery);
		when(fileService.findGalleryById(2L)).thenReturn(null);
		when(accessService.hasModifyPermission(101L)).thenReturn(true);
		doNothing().when(publishServiceSpy)
				   .publishGallery(1L);

		PublishResponse response = publishServiceSpy.publishSelectedGalleries(List.of(1L, 2L));

		assertNotNull(response);
		assertEquals(PublishResultEnum.OK, response.getResult());
		assertEquals("Published 1 galleries, skipped 1", response.getMessage());
	}

	@Test
	void publishSelectedGalleriesReturnsFailWhenNonePublished() throws Exception {
		when(fileService.findGalleryById(anyLong())).thenReturn(null);

		PublishResponse response = publishServiceSpy.publishSelectedGalleries(List.of(5L));

		assertEquals(PublishResultEnum.FAIL, response.getResult());
	}
}

