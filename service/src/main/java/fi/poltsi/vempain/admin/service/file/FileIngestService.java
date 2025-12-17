package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.api.request.file.FileIngestRequest;
import fi.poltsi.vempain.admin.api.response.file.FileIngestResponse;
import fi.poltsi.vempain.admin.configuration.StorageDirectoryConfiguration;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.exception.VempainIngestException;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.admin.service.SubjectService;
import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import fi.poltsi.vempain.tools.LocalFileTools;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import static fi.poltsi.vempain.auth.tools.JsonTools.toJson;
import static fi.poltsi.vempain.tools.LocalFileTools.createAndVerifyDirectory;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileIngestService {
	private final SiteFileRepository siteFileRepository;
	private final GalleryRepository  galleryRepository;
	private final AclService    aclService;
	private final AccessService accessService;

	private final GalleryFileService galleryFileService;

	private final StorageDirectoryConfiguration storageDirectoryConfiguration;
	private final SubjectService subjectService;
	private final FileService    fileService;
	private final LocationService locationService;

	@Value("${vempain.admin.file.site-file-directory}")
	private String siteFileDirectory;

	@PostConstruct
	public void setupEnv() {
		var currentPath = System.getProperty("user.dir");
		log.info("Current directory: {}", currentPath);
		var exceptionMessage = "Unable to initiate the main storage directory";
		var siteFilePath = Path.of(siteFileDirectory);

		if (!siteFilePath.toFile()
						 .exists()) {
			try {
				createAndVerifyDirectory(siteFilePath);
			} catch (Exception e) {
				log.error("Could not create converted main site file storage: {}", siteFilePath);
				throw new FileSystemNotFoundException(exceptionMessage);
			}
		}

		if (!Files.isReadable(siteFilePath)) {
			log.error("Site file main file storage exists  but it has wrong permission: {}", siteFilePath);
			throw new FileSystemNotFoundException(exceptionMessage);
		}
	}

	// Public entry point: delegates to internal ingest and ensures cleanup on failure
	@Transactional
	public FileIngestResponse ingest(FileIngestRequest fileIngestRequest, MultipartFile multipartFile) throws Exception {
		try {
			return ingestInternal(fileIngestRequest, multipartFile);
		} catch (VempainIngestException vex) {
			// Attempt to delete locally stored file if present
			Path stored = vex.getStoredFile();
			if (stored != null) {
				try {
					if (Files.deleteIfExists(stored)) {
						log.info("Deleted stored file after ingest failure: {}", stored);
					}
				} catch (IOException ioe) {
					log.warn("Failed to delete stored file after ingest failure: {}", stored, ioe);
				}
			}
			// Rethrow the original cause if it is a checked Exception, else rethrow the wrapper
			Throwable cause = vex.getCause();
			if (cause instanceof Exception) {
				throw (Exception) cause;
			}
			throw vex;
		}
	}

	// Internal implementation: wraps any failure into VempainIngestException including the stored file path
	@Transactional
	protected FileIngestResponse ingestInternal(FileIngestRequest fileIngestRequest, MultipartFile multipartFile) throws VempainIngestException {
		Path storedFile = null;
		Long galleryId = null;

		try {
			ValidateFileIngestRequest(fileIngestRequest, multipartFile);

			// Determine main class directory by mimetype (fallback to "other" if configured)
			final var fileTypeByMimetype = FileTypeEnum.getFileTypeByMimetype(fileIngestRequest.getMimeType());
			final String baseDir = resolveBaseDir(fileTypeByMimetype);
			log.info("Resolved base directory for file type {}: {}", fileTypeByMimetype, baseDir);

			// Sanitize and resolve target paths
			final String cleanFileName = sanitizeFileName(fileIngestRequest.getFileName());
			final String cleanRelPath = sanitizeRelativePath(Optional.ofNullable(fileIngestRequest.getFilePath())
																	 .orElse(""));
			final Path basePath = Paths.get(baseDir)
									   .toAbsolutePath()
									   .normalize();
			final Path targetDir = basePath.resolve(cleanRelPath)
										   .normalize();

			ensureWithinBase(targetDir, basePath);
			log.info("Creating target directory: {}", targetDir);
			Files.createDirectories(targetDir);

			final Path targetFile = targetDir.resolve(cleanFileName)
											 .normalize();
			ensureWithinBase(targetFile.getParent(), basePath);

			final boolean siteFileExisted = Files.exists(targetFile);
			Files.copy(multipartFile.getInputStream(), targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			// Mark stored file for potential cleanup
			storedFile = targetFile;

			// Verify that the sha256 checksum of the created file matches the provided checksum
			var checksum = LocalFileTools.computeSha256(targetFile.toFile());
			if (!fileIngestRequest.getSha256sum()
								  .equals(checksum)) {
				log.error("SHA-256 checksum mismatch for file: {}. Expected: {}, Actual: {}",
						  targetFile, fileIngestRequest.getSha256sum(), checksum);
				throw new VempainIngestException("SHA parity check failed", null, null);
			}

			final long size = multipartFile.getSize();
			final Instant now = Instant.now();

			// Fetch user account
			var userId = accessService.getUserId();

			if (userId == null || userId < 1) {
				throw new VempainIngestException("Unauthorized S2S call: user ID mismatch", null, storedFile);
			}

			// Upsert SiteFile entity
			var siteFile = siteFileRepository.findByFilePathAndFileName(cleanRelPath, cleanFileName)
											 .orElseGet(SiteFile::new);

			siteFile.setFileName(cleanFileName);
			siteFile.setFilePath(cleanRelPath);
			siteFile.setMimeType(fileIngestRequest.getMimeType());
			siteFile.setFileType(FileTypeEnum.getFileTypeByMimetype(fileIngestRequest.getMimeType()));
			siteFile.setSize(size);
			siteFile.setSha256sum(fileIngestRequest.getSha256sum());
			siteFile.setComment(fileIngestRequest.getComment());
			siteFile.setMetadata(fileIngestRequest.getMetadata());
			siteFile.setHeight(fileIngestRequest.getHeight());
			siteFile.setWidth(fileIngestRequest.getWidth());
			siteFile.setLength(fileIngestRequest.getLength());
			siteFile.setPages(fileIngestRequest.getPages());

			// Populate new fields
			siteFile.setOriginalDateTime(fileIngestRequest.getOriginalDateTime());
			var copyrightRequest = fileIngestRequest.getCopyright();

			if (copyrightRequest != null) {
				siteFile.setRightsHolder(copyrightRequest.getRightsHolder());
				siteFile.setRightsTerms(copyrightRequest.getRightsTerms());
				siteFile.setRightsUrl(copyrightRequest.getRightsUrl());
				siteFile.setCreatorName(copyrightRequest.getCreatorName());
				siteFile.setCreatorEmail(copyrightRequest.getCreatorEmail());
				siteFile.setCreatorCountry(copyrightRequest.getCreatorCountry());
				siteFile.setCreatorUrl(copyrightRequest.getCreatorUrl());
			} else {
				siteFile.setRightsHolder(null);
				siteFile.setRightsTerms(null);
				siteFile.setRightsUrl(null);
				siteFile.setCreatorName(null);
				siteFile.setCreatorEmail(null);
				siteFile.setCreatorCountry(null);
				siteFile.setCreatorUrl(null);
			}

			// Upsert and link GPS location (shared by multiple files)
			var location = locationService.upsertAndGet(fileIngestRequest.getLocation());
			siteFile.setLocation(location);

			if (siteFile.getId() == null) {
				var nextAclId = aclService.createNewAcl(userId, null, true, true, true, true);
				siteFile.setAclId(nextAclId);

				siteFile.setCreator(userId);
				siteFile.setCreated(now);
			} else {
				siteFile.setModifier(userId);
				siteFile.setModified(now);
			}

			log.debug("Storing new SiteFile: {}", siteFile);
			siteFile = fileService.saveSiteFile(siteFile);

			log.debug("Save the tags: {}", fileIngestRequest.getTags());
			subjectService.saveTagsAsSubjects(fileIngestRequest.getTags(), siteFile.getId());

			// Upsert Gallery per requirements
			var gallery = upsertGallery(fileIngestRequest, userId);

			// Add link between SiteFile and Gallery if gallery specified
			if (gallery != null) {
				galleryId = gallery.getId();

				var galleryFileList = galleryFileService.findGalleryFileByGalleryId(gallery.getId());
				var finalSiteFile = siteFile;
				boolean alreadyLinked = galleryFileList.stream()
													   .anyMatch(gf -> Objects.equals(gf.getSiteFileId(), finalSiteFile.getId()));
				if (!alreadyLinked) {
					galleryFileService.addGalleryFile(gallery.getId(), siteFile.getId(), fileIngestRequest.getSortOrder());
				}
			}

			return FileIngestResponse.builder()
									 .galleryId(galleryId)
									 .siteFileId(siteFile.getId())
									 .updated(siteFileExisted)
									 .build();
		} catch (Exception e) {
			throw new VempainIngestException("Ingest failed", e, storedFile);
		}
	}

	private void ValidateFileIngestRequest(FileIngestRequest fileIngestRequest, MultipartFile multipartFile) {
		if (fileIngestRequest == null || multipartFile == null || multipartFile.isEmpty()) {
			throw new IllegalArgumentException("Missing payload");
		}
		if (fileIngestRequest.getFileName() == null || fileIngestRequest.getFileName()
																		.isBlank()) {
			throw new IllegalArgumentException("Missing file name");
		}
		if (fileIngestRequest.getMimeType() == null || fileIngestRequest.getMimeType()
																		.isBlank() || !fileIngestRequest.getMimeType()
																										.contains("/")) {
			throw new IllegalArgumentException("Invalid mimetype");
		}

		// Calculate SHA-256 checksum of the file
		if (fileIngestRequest.getSha256sum() == null || fileIngestRequest.getSha256sum()
																		 .isBlank()) {
			throw new IllegalArgumentException("Missing SHA-256 checksum");
		}
	}

	private String resolveBaseDir(FileTypeEnum fileTypeEnum) {
		var fileType = fileTypeEnum.shortName;
		log.info("Resolving base directory for file class: {}", fileType);
		var storageLocations = storageDirectoryConfiguration.storageLocations();
		log.info("Checking if {} contains {}", storageLocations, fileType);

		if (storageLocations.containsKey(fileType)) {
			return storageLocations.get(fileType);
		}
		if (storageLocations.containsKey("other")) {
			return storageLocations.get("other");
		}

		throw new IllegalArgumentException("Unsupported FileTypeEnum class: " + fileTypeEnum);
	}

	private String sanitizeFileName(String name) {
		var onlyName = Paths.get(name)
							.getFileName()
							.toString();
		if (onlyName.contains("..") || onlyName.contains("/") || onlyName.contains("\\") || onlyName.isBlank()) {
			throw new IllegalArgumentException("Illegal file name");
		}
		return onlyName;
	}

	private String sanitizeRelativePath(String rel) {
		var normalized = Paths.get(rel)
							  .normalize()
							  .toString();
		if (normalized.contains("..") || normalized.startsWith("/") || normalized.startsWith("\\")) {
			throw new IllegalArgumentException("Illegal file path");
		}
		return normalized;
	}

	private void ensureWithinBase(Path target, Path base) {
		if (!target.toAbsolutePath()
				   .normalize()
				   .startsWith(base)) {
			throw new IllegalArgumentException("Attempt to escape base directory");
		}
	}

	@Transactional
	protected Gallery upsertGallery(FileIngestRequest fileIngestRequest, long userId) {
		// If gallery ID exists, update fields if changed; otherwise create if name/description is provided
		Optional<Gallery> optionalGallery = Optional.empty();

		if (fileIngestRequest.getGalleryId() != null) {
			optionalGallery = galleryRepository.findById(fileIngestRequest.getGalleryId());
		} else if (fileIngestRequest.getGalleryName() != null
				   && !fileIngestRequest.getGalleryName()
									 .trim()
									 .isBlank()) {
			optionalGallery = galleryRepository.findByShortname(fileIngestRequest.getGalleryName());
		}

		if (optionalGallery.isPresent()) {
			var gallery = optionalGallery.get();
			log.info("Found existing gallery for ingest request: {}", gallery);
			boolean changed = false;

			if (fileIngestRequest.getGalleryName() != null && !Objects.equals(gallery.getShortname(), fileIngestRequest.getGalleryName())) {
				gallery.setShortname(fileIngestRequest.getGalleryName());
				changed = true;
			}

			if (fileIngestRequest.getGalleryDescription() != null && !Objects.equals(gallery.getDescription(), fileIngestRequest.getGalleryDescription())) {
				gallery.setDescription(fileIngestRequest.getGalleryDescription());
				changed = true;
			}

			if (changed) {
				gallery = galleryRepository.save(gallery);
			}

			return gallery;
		}

		log.debug("No gallery ID given, creating new gallery if name/description provided in request: {}", toJson(fileIngestRequest));

		if ((fileIngestRequest.getGalleryName() != null
			 && !fileIngestRequest.getGalleryName()
								  .isBlank())
			|| (fileIngestRequest.getGalleryDescription() != null
				&& !fileIngestRequest.getGalleryDescription()
									 .isBlank())) {
			log.debug("Creating new gallery for ingest request: {}", toJson(fileIngestRequest));
			// Fetch new acl for the gallery
			long aclId = 0L;
			try {
				aclId = aclService.createNewAcl(userId, null, true, true, true, true);
			} catch (Exception e) {
				log.error("Failed to create ACL for new gallery during ingest", e);
				return null;
			}

			var gallery = Gallery.builder()
								 .shortname(fileIngestRequest.getGalleryName())
								 .description(fileIngestRequest.getGalleryDescription())
								 .siteFiles(new java.util.ArrayList<>())
								 .aclId(aclId)
								 .creator(userId)
								 .locked(false)
								 .created(Instant.now())
								 .build();
			return galleryRepository.save(gallery);
		}

		return null;
	}
}
