package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.api.response.file.GalleryPageResponse;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.service.AclService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GalleryServiceSearchUTC {
	@Mock
	private GalleryRepository  galleryRepository;
	@Mock
	private SiteFileRepository siteFileRepository;
	@Mock
	private GalleryFileService galleryFileService;
	@Mock
	private AclService         aclService;
	@Mock
	private AccessService      accessService;

	@InjectMocks
	private GalleryService galleryService;

	private Gallery sampleGallery;

	@BeforeEach
	void setUp() {
		sampleGallery = Gallery.builder()
							   .id(1L)
							   .shortname("Test")
							   .description("Gallery")
							   .aclId(10L)
							   .creator(1L)
							   .created(Instant.now())
							   .build();
	}

	@Test
	void searchGalleriesReturnsPage() {
		when(galleryRepository.searchGalleries(eq("foo"), eq(false), any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(sampleGallery), PageRequest.of(0, 25, Sort.by("id")), 1));
		when(accessService.hasReadPermission(10L)).thenReturn(true);
		when(aclService.findAclByAclId(10L)).thenReturn(List.of(Acl.builder()
																   .aclId(10L)
																   .build()));

		GalleryPageResponse resp = galleryService.searchGalleries(0, 25, "id", "asc", "foo", false);

		assertNotNull(resp);
		assertEquals(1, resp.getTotalElements());
		assertEquals(1, resp.getItems()
							.size());
	}
}

