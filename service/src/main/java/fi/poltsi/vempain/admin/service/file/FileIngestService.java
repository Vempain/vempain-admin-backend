package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.api.request.file.FileIngestRequest;
import fi.poltsi.vempain.admin.api.response.file.FileIngestResponse;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileIngestService {
	private final Map<String, String> siteStorageLocations;
	private final SiteFileRepository  siteFileRepository;
	private final GalleryRepository   galleryRepository;

	@Value("${vempain.admin.service-psk}")
	private String expectedPsk;

	public FileIngestResponse ingest(FileIngestRequest meta, MultipartFile file, String providedPsk) throws Exception {
		requireValidPsk(providedPsk);
		validateMeta(meta, file);

		// Determine main class directory by mimetype (fallback to "other" if configured)
		final String mainType = extractMainType(meta.getMimeType());
		final String baseDir = resolveBaseDir(mainType);

		// Sanitize and resolve target paths
		final String cleanFileName = sanitizeFileName(meta.getFileName());
		final String cleanRelPath = sanitizeRelativePath(Optional.ofNullable(meta.getFilePath())
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
		Files.copy(file.getInputStream(), targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

		final long size = file.getSize();
		final Instant now = Instant.now();
		final String actor = String.valueOf(meta.getUserId());

		// Upsert SiteFile entity
		var siteFile = siteFileRepository.findByFilePathAndFileName(cleanRelPath, cleanFileName)
										 .orElseGet(SiteFile::new);

		siteFile.setFileName(cleanFileName);
		siteFile.setFilePath(cleanRelPath);
		siteFile.setMimeType(meta.getMimeType());
		siteFile.setSize(size);

		if (siteFile.getId() == null) {
			siteFile.setCreator(actor);
			siteFile.setCreated(now);
		} else {
			siteFile.setUpdater(actor);
			siteFile.setUpdated(now);
		}

		siteFile = siteFileRepository.save(siteFile);

		// Upsert Gallery per requirements
		upsertGallery(meta);

		return new FileIngestResponse(siteFile.getId(), existed);
	}

	private void requireValidPsk(String providedPsk) {
		if (expectedPsk == null || expectedPsk.isBlank() || !Objects.equals(expectedPsk, providedPsk)) {
			throw new AccessDeniedException("Invalid PSK");
		}
	}

	private void validateMeta(FileIngestRequest meta, MultipartFile file) {
		if (meta == null || file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Missing payload");
		}
		if (meta.getFileName() == null || meta.getFileName()
											  .isBlank()) {
			throw new IllegalArgumentException("Missing file name");
		}
		if (meta.getMimeType() == null || meta.getMimeType()
											  .isBlank() || !meta.getMimeType()
																 .contains("/")) {
			throw new IllegalArgumentException("Invalid mimetype");
		}
		if (meta.getUserId() == null) {
			throw new IllegalArgumentException("Missing user ID");
		}
	}

	private String extractMainType(String mime) {
		int idx = mime.indexOf('/');
		return (idx > 0 ? mime.substring(0, idx) : mime).toLowerCase();
	}

	private String resolveBaseDir(String mainType) {
		if (siteStorageLocations.containsKey(mainType)) {
			return siteStorageLocations.get(mainType);
		}
		if (siteStorageLocations.containsKey("other")) {
			return siteStorageLocations.get("other");
		}
		throw new IllegalArgumentException("Unsupported mimetype class: " + mainType);
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

	private void upsertGallery(FileIngestRequest meta) {
		// If gallery ID exists, update fields if changed; otherwise create if name/description is provided
		if (meta.getGalleryId() != null) {
			var opt = galleryRepository.findById(meta.getGalleryId());
			if (opt.isPresent()) {
				var gallery = opt.get();
				boolean changed = false;

				if (meta.getGalleryName() != null && !Objects.equals(gallery.getShortname(), meta.getGalleryName())) {
					gallery.setShortname(meta.getGalleryName());
					changed = true;
				}
				if (meta.getGalleryDescription() != null && !Objects.equals(gallery.getDescription(), meta.getGalleryDescription())) {
					gallery.setDescription(meta.getGalleryDescription());
					changed = true;
				}
				if (changed) {
					galleryRepository.save(gallery);
				}
				return;
			}
			// fallthrough: ID provided but not found -> create
		}

		if ((meta.getGalleryName() != null && !meta.getGalleryName()
												   .isBlank())
			|| (meta.getGalleryDescription() != null && !meta.getGalleryDescription()
															 .isBlank())) {
			var g = new Gallery();
			g.setShortname(meta.getGalleryName());
			g.setDescription(meta.getGalleryDescription());
			galleryRepository.save(g);
		}
	}
}

