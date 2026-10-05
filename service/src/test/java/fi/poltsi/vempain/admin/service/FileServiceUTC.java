package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.api.request.file.SiteFilePagedRequest;
import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.admin.entity.file.FileThumb;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.entity.file.GalleryFile;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.repository.file.FileThumbPageableRepository;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.repository.file.SubjectRepository;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.admin.service.file.FileThumbService;
import fi.poltsi.vempain.admin.service.file.GalleryFileService;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceUTC {
	@Mock
	private AclService                  aclService;
	@Mock
	private FileThumbPageableRepository fileThumbPageableRepository;
	@Mock
	private GalleryRepository           galleryRepository;
	@Mock
	private GalleryFileService          galleryFileService;
	@Mock
	private AccessService               accessService;
	@Mock
	private SubjectRepository           subjectRepository;
	@Mock
	private FileThumbService            fileThumbService;
	@Mock
	private SubjectService              subjectService;
	@Mock
	private PageService                 pageService;
	@Mock
	private PageGalleryService          pageGalleryService;
	@Mock
	private SiteFileRepository          siteFileRepository;

	@InjectMocks
	private FileService fileService;

	@Test
	void findAllGalleriesOk() {
		String shortName = "Test Gallery";
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
		String shortName = "Test Gallery";
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

		List<SiteFile> siteFiles = new ArrayList<>();

		for (int i = 0; i < 4; i++) {
			var siteFile = SiteFile.builder()
								   .id(Integer.toUnsignedLong(i))
								   .fileType(FileTypeEnum.IMAGE)
								   .metadata("Metadata " + i)
								   .build();
			siteFiles.add(siteFile);
		}

		doNothing().when(galleryFileService)
				   .addGalleryFile(anyLong(), anyLong(), anyLong());

		Gallery returnGallery = fileService.createGallery(shortName, description, 1L, siteFiles);
		assertNotNull(returnGallery);
		assertEquals(description, returnGallery.getDescription());
		assertEquals(shortName, returnGallery.getShortname());
	}

	@Test
	void createEmptyGalleryOk() {
		String shortName = "Test Gallery";
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

	@Test
	void findGalleryByIdOk() {
		var gallery = Gallery.builder().id(1L).shortname("Test").build();
		when(galleryRepository.findById(1L)).thenReturn(Optional.of(gallery));
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(List.of());
		when(siteFileRepository.findByIdIn(any())).thenReturn(List.of());

		Gallery result = fileService.findGalleryById(1L);

		assertNotNull(result);
		assertEquals(1L, result.getId());
	}

	@Test
	void findGalleryByIdNotFoundReturnsNullOk() {
		when(galleryRepository.findById(99L)).thenReturn(Optional.empty());

		Gallery result = fileService.findGalleryById(99L);

		assertNull(result);
	}

	@Test
	void findGalleryByIdWithSiteFilesOk() {
		var gallery = Gallery.builder().id(1L).shortname("Test").build();
		var galleryFile = GalleryFile.builder().galleryId(1L).siteFileId(5L).build();
		var siteFile = SiteFile.builder().id(5L).build();

		when(galleryRepository.findById(1L)).thenReturn(Optional.of(gallery));
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(List.of(galleryFile));
		when(siteFileRepository.findByIdIn(any())).thenReturn(List.of(siteFile));

		Gallery result = fileService.findGalleryById(1L);

		assertNotNull(result);
		assertNotNull(result.getSiteFiles());
	}

	@Test
	void findAllSiteFilesOk() {
		when(siteFileRepository.findAll()).thenReturn(List.of(SiteFile.builder().id(1L).build()));

		Iterable<SiteFile> result = fileService.findAllSiteFiles();

		assertNotNull(result);
		assertTrue(result.iterator().hasNext());
	}

	@Test
	void findSiteFileByIdFoundOk() {
		var siteFile = SiteFile.builder().id(1L).build();
		when(siteFileRepository.findById(1L)).thenReturn(Optional.of(siteFile));

		Optional<SiteFile> result = fileService.findSiteFileById(1L);

		assertTrue(result.isPresent());
		assertEquals(1L, result.get().getId());
	}

	@Test
	void findSiteFileByIdNotFoundOk() {
		when(siteFileRepository.findById(99L)).thenReturn(Optional.empty());

		Optional<SiteFile> result = fileService.findSiteFileById(99L);

		assertTrue(result.isEmpty());
	}

	@Test
	void saveSiteFileOk() {
		var siteFile = SiteFile.builder().id(1L).build();
		when(siteFileRepository.saveAndFlush(any(SiteFile.class))).thenReturn(siteFile);

		SiteFile result = fileService.saveSiteFile(siteFile);

		assertNotNull(result);
		assertEquals(1L, result.getId());
		verify(siteFileRepository).saveAndFlush(siteFile);
	}

	@Test
	void findAllSiteFileIdWithSubjectOk() {
		when(siteFileRepository.findAllSiteFileIdWithSubject()).thenReturn(List.of(1L, 2L, 3L));

		Set<Long> result = fileService.findAllSiteFileIdWithSubject();

		assertNotNull(result);
		assertEquals(3, result.size());
	}

	@Test
	void removeFileSubjectsOk() {
		// method body is empty - just verify it doesn't throw
		try {
			fileService.removeFileSubjects(Set.of(1L, 2L));
		} catch (Exception e) {
			fail("Should not have received an exception", e);
		}
	}

	@Test
	void findAllFileThumbsOk() {
		var thumb = FileThumb.builder().parentId(1L).build();
		when(fileThumbPageableRepository.findAll()).thenReturn(List.of(thumb));

		Iterable<FileThumb> result = fileService.findAllFileThumbs();

		assertNotNull(result);
		assertTrue(result.iterator().hasNext());
	}

	@Test
	void findAllFileThumbsByFilepathFilenameOk() {
		var thumb = FileThumb.builder().parentId(1L).build();
		when(fileThumbPageableRepository.findAllByFilepathAndFilename("path", "file.jpg"))
				.thenReturn(List.of(thumb));

		Iterable<FileThumb> result = fileService.findAllFileThumbsByFilepathFilename("path", "file.jpg");

		assertNotNull(result);
		assertTrue(result.iterator().hasNext());
	}

	@Test
	void getDuplicateThumbFilesNoneOk() {
		var thumb1 = FileThumb.builder().parentId(1L).filepath("a").filename("f1.jpg").build();
		var thumb2 = FileThumb.builder().parentId(2L).filepath("b").filename("f2.jpg").build();
		when(fileThumbPageableRepository.findAll()).thenReturn(List.of(thumb1, thumb2));

		List<FileThumb> result = fileService.getDuplicateThumbFiles();

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void getDuplicateThumbFilesDuplicatesFoundOk() {
		var thumb1 = FileThumb.builder().parentId(1L).filepath("a").filename("f.jpg").build();
		var thumb2 = FileThumb.builder().parentId(2L).filepath("a").filename("f.jpg").build();
		when(fileThumbPageableRepository.findAll()).thenReturn(List.of(thumb1, thumb2));

		List<FileThumb> result = fileService.getDuplicateThumbFiles();

		assertNotNull(result);
		assertEquals(2, result.size());
	}

	@Test
	void deleteFileThumbOk() {
		var thumb = FileThumb.builder().parentId(1L).build();
		doNothing().when(fileThumbPageableRepository).delete(thumb);

		fileService.deleteFileThumb(thumb);

		verify(fileThumbPageableRepository).delete(thumb);
	}

	@Test
	void saveSubjectOk() {
		var subject = Subject.builder().subjectName("Test Subject").build();
		when(subjectRepository.save(any(Subject.class))).thenReturn(subject);

		Subject result = fileService.saveSubject(subject);

		assertNotNull(result);
		verify(subjectRepository).save(subject);
	}

	@Test
	void findAllFileThumbsBySiteFileListEmptyOk() {
		when(fileThumbPageableRepository.findFileThumbByParentId(1L)).thenReturn(Optional.empty());
		var siteFile = SiteFile.builder().id(1L).build();

		List<FileThumb> result = fileService.findAllFileThumbsBySiteFileList(List.of(siteFile));

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void findAllFileThumbsBySiteFileListFoundOk() {
		var siteFile = SiteFile.builder().id(1L).build();
		var thumb = FileThumb.builder().parentId(1L).build();
		when(fileThumbPageableRepository.findFileThumbByParentId(1L)).thenReturn(Optional.of(thumb));

		List<FileThumb> result = fileService.findAllFileThumbsBySiteFileList(List.of(siteFile));

		assertNotNull(result);
		assertEquals(1, result.size());
	}

	@Test
	void refreshGalleryFilesOk() {
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(List.of());

		var result = fileService.refreshGalleryFiles(1L);

		assertNotNull(result);
	}

	@Test
	void refreshAllGalleryFilesOk() {
		when(galleryRepository.getAllGalleryIds()).thenReturn(List.of(1L, 2L));
		when(galleryFileService.findGalleryFileByGalleryId(1L)).thenReturn(List.of());
		when(galleryFileService.findGalleryFileByGalleryId(2L)).thenReturn(List.of());

		var result = fileService.refreshAllGalleryFiles();

		assertNotNull(result);
	}

	// ---- findAllSiteFilesAsPageableResponseFiltered: every listing is ACL filtered through accessService.readableSpecification() ----

	@SuppressWarnings("unchecked")
	private PageRequest stubReadableListing(PageRequest pageRequest) {
		var emptyPage = new PageImpl<SiteFile>(List.of(), pageRequest, 0);
		when(accessService.<SiteFile>readableSpecification()).thenReturn((root, query, criteriaBuilder) -> null);
		when(siteFileRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);
		return pageRequest;
	}

	@SuppressWarnings("unchecked")
	private void verifyReadableListing() {
		verify(accessService).<SiteFile>readableSpecification();
		verify(siteFileRepository).findAll(any(Specification.class), any(Pageable.class));
	}

	@Test
	void findAllSiteFilesAsPageableResponseFiltered_nullFilterColumn_usesAclFilteredQuery() {
		var pageRequest = stubReadableListing(PageRequest.of(0, 10));

		var result = fileService.findAllSiteFilesAsPageableResponseFiltered(request(FileTypeEnum.IMAGE, pageRequest, "test", null));

		assertNotNull(result);
		verifyReadableListing();
	}

	@Test
	void findAllSiteFilesAsPageableResponseFiltered_blankFilter_usesAclFilteredQuery() {
		var pageRequest = stubReadableListing(PageRequest.of(0, 10));

		var result = fileService.findAllSiteFilesAsPageableResponseFiltered(request(FileTypeEnum.IMAGE, pageRequest, "  ", "filename"));

		assertNotNull(result);
		verifyReadableListing();
	}

	@Test
	void findAllSiteFilesAsPageableResponseFiltered_everyColumnFilterStaysAclFiltered() {
		for (var column : List.of("filename", "file_name", "FileName", "filepath", "mimetype", "unknownColumn", "size")) {
			org.mockito.Mockito.reset(accessService, siteFileRepository);
			var pageRequest = stubReadableListing(PageRequest.of(0, 10));

			var result = fileService.findAllSiteFilesAsPageableResponseFiltered(request(FileTypeEnum.IMAGE, pageRequest, "1024", column));

			assertNotNull(result);
			verifyReadableListing();
		}
	}

	@Test
	void findAllSiteFilesAsPageableResponseFiltered_temporalColumns_parseIsoInstant() {
		for (var column : List.of("created", "modified")) {
			org.mockito.Mockito.reset(accessService, siteFileRepository);
			var pageRequest = stubReadableListing(PageRequest.of(0, 10));

			var result = fileService.findAllSiteFilesAsPageableResponseFiltered(request(FileTypeEnum.IMAGE, pageRequest, "2023-01-01T00:00:00Z", column));

			assertNotNull(result);
			verifyReadableListing();
		}
	}

	@Test
	void findAllSiteFilesAsPageableResponseFiltered_subjectColumn_resolvesSiteFileIdsBySubject() {
		var pageRequest = stubReadableListing(PageRequest.of(0, 10));
		when(siteFileRepository.findSiteFileIdsBySubjectNameContaining("test")).thenReturn(List.of(5L, 6L));

		var result = fileService.findAllSiteFilesAsPageableResponseFiltered(request(FileTypeEnum.IMAGE, pageRequest, "test", "subject"));

		assertNotNull(result);
		verify(siteFileRepository).findSiteFileIdsBySubjectNameContaining("test");
		verifyReadableListing();
	}

	@Test
	void findAllSiteFilesAsPageableResponseFiltered_invalidSizeColumn_fallsBackToClassOnlyListing() {
		var pageRequest = stubReadableListing(PageRequest.of(0, 10));

		var result = fileService.findAllSiteFilesAsPageableResponseFiltered(request(FileTypeEnum.IMAGE, pageRequest, "notANumber", "size"));

		assertNotNull(result);
		verifyReadableListing();
	}

	@Test
	void findAllSiteFilesAsPageableResponseFiltered_returnsOnlyRowsFromTheAclFilteredPage() {
		var pageRequest = PageRequest.of(0, 10);
		var readable = SiteFile.builder()
							   .id(1L)
							   .aclId(11L)
							   .creator(1L)
							   .created(Instant.now())
							   .fileName("readable.jpg")
							   .filePath("/a")
							   .mimeType("image/jpeg")
							   .fileType(FileTypeEnum.IMAGE)
							   .comment("")
							   .metadata("{}")
							   .sha256sum("x")
							   .build();
		when(accessService.<SiteFile>readableSpecification()).thenReturn((root, query, criteriaBuilder) -> null);
		when(siteFileRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(readable), pageRequest, 1));

		var result = fileService.findAllSiteFilesAsPageableResponseFiltered(request(FileTypeEnum.IMAGE, pageRequest, null, null));

		assertEquals(1, result.getContent()
							  .size());
		assertEquals(1L, result.getContent()
							   .getFirst()
							   .getId());
	}

	@Test
	void findAllSiteFilesAsPageableResponseFiltered_sortByCreatedAt_remapped() {
		var pageRequest = stubReadableListing(PageRequest.of(0, 10, Sort.by("createdAt")));

		var result = fileService.findAllSiteFilesAsPageableResponseFiltered(request(FileTypeEnum.IMAGE, pageRequest, null, null));

		assertNotNull(result);
		var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
		verify(siteFileRepository).findAll(any(Specification.class), pageableCaptor.capture());
		assertNotNull(pageableCaptor.getValue()
									.getSort()
									.getOrderFor("created"));
	}

	@Test
	void findAllSiteFilesAsPageableResponseFiltered_sortByModifiedAt_remapped() {
		var pageRequest = stubReadableListing(PageRequest.of(0, 10, Sort.by("modifiedAt")));

		var result = fileService.findAllSiteFilesAsPageableResponseFiltered(request(FileTypeEnum.IMAGE, pageRequest, null, null));

		assertNotNull(result);
		var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
		verify(siteFileRepository).findAll(any(Specification.class), pageableCaptor.capture());
		assertNotNull(pageableCaptor.getValue()
									.getSort()
									.getOrderFor("modified"));
	}

	@Test
	void findAllSiteFilesAsPageableResponseFiltered_sortBySnakeCaseFileName_remapped() {
		var pageRequest = stubReadableListing(PageRequest.of(0, 10, Sort.by("file_name")));

		var result = fileService.findAllSiteFilesAsPageableResponseFiltered(request(FileTypeEnum.IMAGE, pageRequest, null, null));

		assertNotNull(result);
		var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
		verify(siteFileRepository).findAll(any(Specification.class), pageableCaptor.capture());
		assertEquals("fileName", pageableCaptor.getValue()
		                                       .getSort()
		                                       .getOrderFor("fileName")
		                                       .getProperty());
	}

	private SiteFilePagedRequest request(FileTypeEnum fileType, PageRequest pageRequest, String search, String filterColumn) {
		var request = new SiteFilePagedRequest();
		request.setFileType(fileType);
		request.setPage(pageRequest.getPageNumber());
		request.setSize(pageRequest.getPageSize());
		request.setSearch(search);
		request.setFilterColumn(filterColumn);
		var order = pageRequest.getSort()
		                       .stream()
		                       .findFirst();
		order.ifPresent(value -> {
			request.setSortBy(value.getProperty());
			request.setDirection(value.getDirection());
		});
		return request;
	}
}
