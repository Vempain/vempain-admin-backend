package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.entity.file.FileCommon;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.repository.file.FileAudioPageableRepository;
import fi.poltsi.vempain.admin.repository.file.FileCommonPageableRepository;
import fi.poltsi.vempain.admin.repository.file.FileDocumentPageableRepository;
import fi.poltsi.vempain.admin.repository.file.FileImagePageableRepository;
import fi.poltsi.vempain.admin.repository.file.FileThumbPageableRepository;
import fi.poltsi.vempain.admin.repository.file.FileVideoPageableRepository;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.repository.file.SubjectRepository;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.admin.service.file.FileThumbService;
import fi.poltsi.vempain.admin.service.file.GalleryFileService;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceUTC {
	@Mock
	private AclService                     aclService;
	@Mock
	private FileCommonPageableRepository   fileCommonPageableRepository;
	@Mock
	private FileAudioPageableRepository    fileAudioPageableRepository;
	@Mock
	private FileDocumentPageableRepository fileDocumentPageableRepository;
	@Mock
	private FileImagePageableRepository    fileImagePageableRepository;
	@Mock
	private FileVideoPageableRepository    fileVideoPageableRepository;
	@Mock
	private FileThumbPageableRepository    fileThumbPageableRepository;
	@Mock
	private GalleryRepository              galleryRepository;
	@Mock
	private GalleryFileService             galleryFileService;
	@Mock
	private AccessService                  accessService;
	@Mock
	private SubjectRepository              subjectRepository;
	@Mock
	private FileThumbService               fileThumbService;
	@Mock
	private SubjectService                 subjectService;
	@Mock
	private PageService                    pageService;
	@Mock
	private PageGalleryService             pageGalleryService;

	@InjectMocks
	private FileService fileService;

	@Test
	void findAllGalleriesOk() {
		String shortName   = "Test Gallery";
		String description = "Test gallery description";

		List<Gallery> results = new ArrayList<>();
		for (long i = 0; i < 4L; i++) {
			Gallery gallery = Gallery.builder()
									 .shortname(shortName + " " + i)
									 .aclId(i)
									 .description(description + " " + i)
									 .id(i)
									 .creator(i)
									 .created(Instant.now()
													 .minus(1, ChronoUnit.HOURS))
									 .modifier(i)
									 .modified(Instant.now())
									 .build();
			results.add(gallery);
		}

		when(galleryRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))).thenReturn(results);
		List<Gallery> galleries = fileService.findAllGalleries();
		assertNotNull(galleries);
		assertEquals(4L, galleries.size());
	}

	@Test
	void createGalleryOk() {
		String shortName   = "Test Gallery";
		String description = "Test gallery description";

		Gallery gallery = Gallery.builder()
								 .shortname(shortName)
								 .aclId(1L)
								 .description(description)
								 .id(1L)
								 .creator(1L)
								 .created(Instant.now()
												 .minus(1, ChronoUnit.HOURS))
								 .modifier(1L)
								 .modified(Instant.now())
								 .build();

		when(galleryRepository.save(any())).thenReturn(gallery);

		List<FileCommon> fileCommonList = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			FileCommon fileCommon = FileCommon.builder()
											  .id(Integer.toUnsignedLong(i))
											  .fileClassId(1L)
											  .aclId(i)
											  .convertedFile("/source/file/" + i + ".jpg")
											  .convertedFilesize(100L + i)
											  .convertedSha1sum("so-Sha1sum" + i)
											  .siteFilepath("site/file/")
											  .siteFilename(i + ".jpg")
											  .siteFilesize(200L + i)
											  .siteSha1sum("si-Sha1sum" + i)
											  .metadata("Metadata " + i)
											  .build();
			fileCommonList.add(fileCommon);
		}

		doNothing().when(galleryFileService)
				   .addGalleryFile(anyLong(), anyLong(), anyLong());

		Gallery returnGallery = fileService.createGallery(shortName, description, 1L, fileCommonList);
		assertNotNull(returnGallery);
		assertEquals(description, returnGallery.getDescription());
		assertEquals(shortName, returnGallery.getShortname());
	}

	@Test
	void createEmptyGalleryOk() {
		String shortName   = "Test Gallery";
		String description = "Test gallery description";

		Gallery gallery = Gallery.builder()
								 .shortname(shortName)
								 .aclId(1L)
								 .description(description)
								 .id(1L)
								 .creator(1L)
								 .created(Instant.now()
												 .minus(1, ChronoUnit.HOURS))
								 .modifier(1L)
								 .modified(Instant.now())
								 .build();

		when(galleryRepository.save(any())).thenReturn(gallery);

		Gallery returnGallery = fileService.createEmptyGallery(shortName, description, 1L);
		assertNotNull(returnGallery);
		assertEquals(description, returnGallery.getDescription());
		assertEquals(shortName, returnGallery.getShortname());
	}

	@Test
	void saveGalleryOk() {
		var acl = TestUTCTools.generateAcl(1L, 1L, 1L, 1L);
		var gallery = Gallery.builder()
							 .shortname("")
							 .description("")
							 .aclId(acl.getAclId())
							 .build();

		try {
			fileService.saveGallery(gallery);
		} catch (Exception e) {
			fail("Should not have received an exception", e);
		}
	}
}
