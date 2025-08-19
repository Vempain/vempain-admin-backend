package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.api.FileClassEnum;
import fi.poltsi.vempain.admin.api.request.file.FileIngestRequest;
import fi.poltsi.vempain.admin.api.response.file.FileIngestResponse;
import fi.poltsi.vempain.admin.configuration.StorageDirectoryConfiguration;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.tools.LocalFileTools;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import static fi.poltsi.vempain.tools.LocalFileTools.createAndVerifyDirectory;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileIngestService {
	private final SiteFileRepository  siteFileRepository;
	private final GalleryRepository   galleryRepository;
	private final StorageDirectoryConfiguration storageDirectoryConfiguration;

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
				log.error("Could not create converted main file storage: {}", siteFilePath);
				throw new FileSystemNotFoundException(exceptionMessage);
			}
		}

		if (!Files.isReadable(siteFilePath)) {
			log.error("Site file main file storage exists  but it has wrong permission: {}", siteFilePath);
			throw new FileSystemNotFoundException(exceptionMessage);
		}
	}

	public FileIngestResponse ingest(FileIngestRequest fileIngestRequest, MultipartFile multipartFile) throws Exception {
		ValidateFileIngestRequest(fileIngestRequest, multipartFile);

		// Determine main class directory by mimetype (fallback to "other" if configured)
		final var fileClassByMimetype = FileClassEnum.getFileClassByMimetype(fileIngestRequest.getMimeType());
		final String baseDir = resolveBaseDir(fileClassByMimetype);
		log.info("Resolved base directory for file class {}: {}", fileClassByMimetype, baseDir);
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
		Files.createDirectories(targetDir);

		final Path targetFile = targetDir.resolve(cleanFileName)
										 .normalize();
		ensureWithinBase(targetFile.getParent(), basePath);

		final boolean existed = Files.exists(targetFile);
		Files.copy(multipartFile.getInputStream(), targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

		// Verify that the sha256 checksum of the created file matches the provided checksum
		var checksum = LocalFileTools.computeSha256(targetFile.toFile());

		if (!fileIngestRequest.getSha256sum().equals(checksum)) {
			log.error("SHA-256 checksum mismatch for file: {}. Expected: {}, Actual: {}",
					  targetFile, fileIngestRequest.getSha256sum(), checksum);
			throw new IllegalArgumentException("SHA-256 checksum mismatch");
		}

		final long size = multipartFile.getSize();
		final Instant now = Instant.now();

		// Upsert SiteFile entity
		var siteFile = siteFileRepository.findByFilePathAndFileName(cleanRelPath, cleanFileName)
										 .orElseGet(SiteFile::new);

		siteFile.setFileName(cleanFileName);
		siteFile.setFilePath(cleanRelPath);
		siteFile.setMimeType(fileIngestRequest.getMimeType());
		siteFile.setFileClass(FileClassEnum.getFileClassByMimetype(fileIngestRequest.getMimeType()));
		siteFile.setSize(size);
		siteFile.setSha256sum(fileIngestRequest.getSha256sum());

		if (siteFile.getId() == null) {
			siteFile.setCreator(fileIngestRequest.getUserId());
			siteFile.setCreated(now);
		} else {
			siteFile.setModifier(fileIngestRequest.getUserId());
			siteFile.setModified(now);
		}

		siteFile = siteFileRepository.save(siteFile);

		// Upsert Gallery per requirements
		upsertGallery(fileIngestRequest);

		return new FileIngestResponse(siteFile.getId(), existed);
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
		if (fileIngestRequest.getUserId() == null) {
			throw new IllegalArgumentException("Missing user ID");
		}
		// Calculate SHA-256 checksum of the file
		if (fileIngestRequest.getSha256sum() == null || fileIngestRequest.getSha256sum()
											  .isBlank()) {
			throw new IllegalArgumentException("Missing SHA-256 checksum");
		}

	}

	private String resolveBaseDir(FileClassEnum fileClassEnum) {
		var fileClass = fileClassEnum.name().toLowerCase();
		log.info("Resolving base directory for file class: {}", fileClass);
		var storageLocations = storageDirectoryConfiguration.storageLocations();
		log.info("Checking if {} contains {}", storageLocations, fileClass);

		if (storageLocations.containsKey(fileClass)) {
			return storageLocations.get(fileClass);
		}
		if (storageLocations.containsKey("other")) {
			return storageLocations.get("other");
		}

		throw new IllegalArgumentException("Unsupported FileClassEnum class: " + fileClassEnum);
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

	private void upsertGallery(FileIngestRequest fileIngestRequest) {
		// If gallery ID exists, update fields if changed; otherwise create if name/description is provided
		if (fileIngestRequest.getGalleryId() != null) {
			var opt = galleryRepository.findById(fileIngestRequest.getGalleryId());
			if (opt.isPresent()) {
				var gallery = opt.get();
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
					galleryRepository.save(gallery);
				}
				return;
			}
			// fallthrough: ID provided but not found -> create
		}

		if ((fileIngestRequest.getGalleryName() != null && !fileIngestRequest.getGalleryName()
																			 .isBlank())
			|| (fileIngestRequest.getGalleryDescription() != null && !fileIngestRequest.getGalleryDescription()
																					   .isBlank())) {
			var g = new Gallery();
			g.setShortname(fileIngestRequest.getGalleryName());
			g.setDescription(fileIngestRequest.getGalleryDescription());
			galleryRepository.save(g);
		}
	}
}

