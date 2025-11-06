package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.api.FileClassEnum;
import fi.poltsi.vempain.admin.api.PublishResultEnum;
import fi.poltsi.vempain.admin.api.response.RefreshDetailResponse;
import fi.poltsi.vempain.admin.api.response.RefreshResponse;
import fi.poltsi.vempain.admin.api.response.file.SiteFileResponse;
import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.admin.entity.file.FileThumb;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.entity.file.GalleryFile;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.repository.file.FileThumbPageableRepository;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.repository.file.SubjectRepository;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.admin.service.SubjectService;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.service.AclService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class FileService {

	private final FileThumbPageableRepository fileThumbPageableRepository;
	private final GalleryRepository           galleryRepository;
	private final SubjectRepository           subjectRepository;
	private final AclService                  aclService;
	private final AccessService               accessService;
	private final GalleryFileService          galleryFileService;
	private final SubjectService              subjectService;
	private final SiteFileRepository          siteFileRepository;

	@Value("${vempain.admin.file.site-file-directory}")
	private String siteFileDirectory;
	@Value("${vempain.admin.file.image-format}")
	private String imageFormat;

	private static final String RESPONSE_STATUS_EXCEPTION_MESSAGE = "Unknown error";

	// FileGallery
	public List<Gallery> findAllGalleries() {
		return galleryRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
	}

	public Gallery findGalleryById(Long galleryId) {
		var optionalGallery = galleryRepository.findById(galleryId);

		if (optionalGallery.isEmpty()) {
			return null;
		}

		var gallery = optionalGallery.get();
		var galleryFiles = galleryFileService.findGalleryFileByGalleryId(galleryId);
		var siteFileIdList = new ArrayList<Long>();

		for (GalleryFile galleryFile : galleryFiles) {
			siteFileIdList.add(galleryFile.getSiteFileId());
		}

		var commonFiles = siteFileRepository.findByIdIn(siteFileIdList);

		gallery.setSiteFiles(commonFiles);
		return gallery;
	}

	@Transactional
	public Gallery createGallery(String shortName, String description, long userId, List<SiteFile> SiteFileList) {
		long aclId;

		try {
			aclId = aclService.createNewAcl(userId, null, true, true, true, true);
		} catch (VempainAclException e) {
			log.error("Storing ACL list failed for unknown reason: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, RESPONSE_STATUS_EXCEPTION_MESSAGE);
		}

		Gallery gallery = Gallery.builder()
								 .shortname(shortName)
								 .description(description)
								 .siteFiles(new ArrayList<>())
								 .creator(userId)
								 .created(Instant.now())
								 .aclId(aclId)
								 .locked(false)
								 .build();
		var newGallery = galleryRepository.save(gallery);

		long sortOrder = 0;

		for (var siteFile : SiteFileList) {
			galleryFileService.addGalleryFile(newGallery.getId(), siteFile.getId(), sortOrder);
			sortOrder++;
		}

		return gallery;
	}

	@Transactional
	public Gallery createEmptyGallery(String shortName, String description, Long userId) {
		return createGallery(shortName, description, userId, new ArrayList<>());
	}

	@Transactional
	public void saveGallery(Gallery gallery) {
		galleryRepository.save(gallery);
	}

	// FileCommon
	public Iterable<SiteFile> findAllSiteFiles() {
		return siteFileRepository.findAll();
	}

	public Optional<SiteFile> findSiteFileById(long siteFileId) {
		return siteFileRepository.findById(siteFileId);
	}

	@Transactional
	public SiteFile saveSiteFile(SiteFile siteFile) {
		// Ensure the insert is flushed so downstream native queries see the row
		return siteFileRepository.saveAndFlush(siteFile);
	}

	public Set<Long> findAllSiteFileIdWithSubject() {
		var siteFileIdList = siteFileRepository.findAllSiteFileIdWithSubject();

		return new HashSet<>(siteFileIdList);
	}

	private static JSONObject metadataToJsonObject(String metadata) {
		var jsonArray = new JSONArray(metadata);

		if (jsonArray.isEmpty()) {
			log.error("Failed to parse the metadata JSON from\n{}", metadata);
			return null;
		}

		return jsonArray.getJSONObject(0);
	}

	// FileAudio
	public Page<SiteFileResponse> findAllSiteFilesAsPageableResponseFiltered(FileClassEnum fileClassEnum, PageRequest pageRequest, String filter, String filterColumn) {
		Page<SiteFile> siteFiles;
		// Generate required Pageable alongside the incoming PageRequest
		Pageable pageable = PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize(), pageRequest.getSort());

		if (filter == null || filter.isBlank()
			|| filterColumn == null || filterColumn.isBlank()) {
			// Filter only by file class
			siteFiles = siteFileRepository.findByFileClass(fileClassEnum, pageRequest, pageable);
		} else {
			siteFiles = switch (filterColumn) {
				case "filename" -> siteFileRepository.findByFileNameContainingIgnoreCaseAndFileClass(filter, fileClassEnum, pageRequest, pageable);
				case "filepath" -> siteFileRepository.findByFilePathContainingIgnoreCaseAndFileClass(filter, fileClassEnum, pageRequest, pageable);
				case "mimetype" -> siteFileRepository.findByMimeTypeContainingIgnoreCaseAndFileClass(filter, fileClassEnum, pageRequest, pageable);
				case "created" -> siteFileRepository.findByCreatedAfterAndFileClass(Instant.parse(filter), fileClassEnum, pageRequest, pageable);
				case "modified" -> siteFileRepository.findByModifiedAfterAndFileClass(Instant.parse(filter), fileClassEnum, pageRequest, pageable);
				case "subject" -> siteFileRepository.findBySubjectNameContainingIgnoreCaseAndFileClass(filter, fileClassEnum.ordinal(), pageRequest, pageable);
				case "size" -> {
					long sizeFilter;
					try {
						sizeFilter = Long.parseLong(filter);
					} catch (NumberFormatException nfe) {
						log.warn("Invalid size filter '{}', falling back to class-only listing", filter);
						yield siteFileRepository.findByFileClass(fileClassEnum, pageRequest, pageable);
					}
					yield siteFileRepository.findBySizeGreaterThanEqualAndFileClass(sizeFilter, fileClassEnum, pageRequest, pageable);
				}
				default -> siteFileRepository.findByFileClass(fileClassEnum, pageRequest, pageable);
			};
		}

		populateSiteFilePage(siteFiles);
		return siteFiles.map(SiteFile::toResponse);
	}

	private void populateSiteFilePage(Page<SiteFile> siteFilePage) {
		// No additional enrichment required for SiteFileResponse at this time.
		// Method kept for future expansion (e.g., preloading related data).
	}

	// FileSubject
	public void removeFileSubjects(Set<Long> fileSubjectIdSet) {
		// fileCommonPageableRepository.deleteAllBySubjectId(fileSubjectIdSet);
	}

	// ThumbFiles
	public Iterable<FileThumb> findAllFileThumbs() {
		return fileThumbPageableRepository.findAll();
	}

	public Iterable<FileThumb> findAllFileThumbsByFilepathFilename(String filepath, String filename) {
		return fileThumbPageableRepository.findAllByFilepathAndFilename(filepath, filename);
	}

	public List<FileThumb> getDuplicateThumbFiles() {
		var thumbFiles = fileThumbPageableRepository.findAll();

		var thumbFileMap = new HashMap<String, FileThumb>();
		var duplicates = new ArrayList<FileThumb>();

		for (FileThumb thumbFile : thumbFiles) {
			var key = thumbFile.getFilepath() + File.separator + thumbFile.getFilename();

			if (thumbFileMap.containsKey(key)) {
				log.info("Duplicate thumb file: {}", thumbFile);
				duplicates.add(thumbFile);
				duplicates.add(thumbFileMap.get(key));
			} else {
				thumbFileMap.put(key, thumbFile);
			}
		}

		return duplicates;
	}

	public void deleteFileThumb(FileThumb fileThumb) {
		fileThumbPageableRepository.delete(fileThumb);
	}

	public List<FileThumb> findAllFileThumbsBySiteFileList(List<SiteFile> siteFiles) {
		var thumbList = new ArrayList<FileThumb>();

		for (var siteFile : siteFiles) {
			var optionalFileThumb = fileThumbPageableRepository.findFileThumbByParentId(siteFile.getId());

			if (optionalFileThumb.isEmpty()) {
				log.error("SiteFile ID {} is missing a thumb file", siteFile.getId());
			} else {
				thumbList.add(optionalFileThumb.get());
			}
		}

		return thumbList;
	}

	// Subject
	public Subject saveSubject(Subject subject) {
		return subjectRepository.save(subject);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public RefreshResponse refreshAllGalleryFiles() {
		var refreshResponse = RefreshResponse.builder()
											 .details(new ArrayList<>())
											 .build();
		var successCount = 0L;
		var failedCount = 0L;
		// First get all the gallery IDs
		var galleryIds = galleryRepository.getAllGalleryIds();
		for (Long galleryId : galleryIds) {
			var galleryResponse = refreshGalleryFiles(galleryId);
			log.debug("Gallery {} refresh result: {}", galleryId, galleryResponse);

			successCount = successCount + galleryResponse.getRefreshedItems();
			failedCount = failedCount + galleryResponse.getFailedItems();
			refreshResponse.getDetails()
						   .addAll(galleryResponse.getDetails());
		}

		refreshResponse.setRefreshedItems(successCount);
		refreshResponse.setFailedItems(failedCount);
		refreshResponse.setResult(failedCount == 0 ? PublishResultEnum.OK : PublishResultEnum.FAIL);
		return refreshResponse;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public RefreshResponse refreshGalleryFiles(long galleryId) {
		var galleryFiles = galleryFileService.findGalleryFileByGalleryId(galleryId);
		var refreshDetails = new ArrayList<RefreshDetailResponse>();
		var successCount = 0;
		var failedCount = 0;
/* TODO fix this later
		for (var galleryFile : galleryFiles) {
			log.debug("Refreshing gallery file: {}", galleryFile);
			var refreshDetail = refreshFile(galleryFile.getSiteFileId());
			refreshDetails.add(refreshDetail);

			if (refreshDetail != null && refreshDetail.getResult() == PublishResultEnum.OK) {
				successCount++;
			} else {
				failedCount++;
			}
		}
*/
		return RefreshResponse.builder()
							  .refreshedItems(successCount)
							  .failedItems(failedCount)
							  .result(failedCount == 0 ? PublishResultEnum.OK : PublishResultEnum.FAIL)
							  .details(refreshDetails)
							  .build();
	}
}
