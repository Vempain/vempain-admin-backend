package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.api.FileClassEnum;
import fi.poltsi.vempain.admin.entity.file.FileThumb;
import fi.poltsi.vempain.admin.repository.file.FileThumbPageableRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.tools.ImageTools;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static fi.poltsi.vempain.tools.LocalFileTools.computeSha256;
import static fi.poltsi.vempain.tools.LocalFileTools.createAndVerifyDirectory;
import static fi.poltsi.vempain.tools.LocalFileTools.getFileSize;
import static fi.poltsi.vempain.tools.LocalFileTools.setExtension;

@Slf4j
@Setter // We use this solely for testing purposes
@RequiredArgsConstructor
@Service
public class FileThumbService {
	private static final String                      RESPONSE_STATUS_EXCEPTION_MESSAGE = "Unknown error";
	private final        FileThumbPageableRepository fileThumbPageableRepository;
	private final        SiteFileRepository          siteFileRepository;
	private final        ImageTools                  imageTools;

	@Value("${vempain.admin.file.site-file-directory}")
	private String siteFileDirectory;
	@Value("${vempain.admin.file.image-format}")
	private String imageFormat;
	@Value("${vempain.admin.file.thumbnail-size}")
	private int    thumbnailSize;


	@Transactional
	public void generateThumbFile(long siteFileId) {
		if (siteFileId < 1) {
			log.error("Site file ID {} is invalid", siteFileId);
			return;
		}

		var optionalSiteFile = siteFileRepository.findById(siteFileId);

		if (optionalSiteFile.isEmpty()) {
			log.warn("Common file ID {} does not exist in database", siteFileId);
			return;
		}

		var siteFile = optionalSiteFile.get();
		var sourcePath =
				Path.of((siteFileDirectory != null ? siteFileDirectory : "") +
						File.separator + siteFile.getFileClass().shortName + File.separator + siteFile.getFilePath() + File.separator + siteFile.getFileName());

		generateThumbFile(siteFile.getId(), sourcePath, Path.of(siteFile.getFilePath()), siteFile.getFileClass());
	}

	@Transactional(propagation = Propagation.REQUIRED)
	protected void generateThumbFile(long commonId, Path sourceFile, Path destination, FileClassEnum fileClassEnum) {
		// We add the thumb class to the beginning of the relative path
		var relativeDestinationPath = Path.of(FileClassEnum.THUMB.shortName + File.separator + fileClassEnum.shortName + File.separator + destination);
		var absoluteDestinationPath = Path.of(siteFileDirectory + File.separator + relativeDestinationPath);
		log.debug("Relative thumb path: {}", relativeDestinationPath);
		log.debug("Absolute thumb path: {}", absoluteDestinationPath);
		// Set the correct file extension
		var thumbFilename = setExtension(sourceFile.getFileName()
												   .toString(), (imageFormat != null ? imageFormat : "jpeg"));

		var destinationFile = Path.of(absoluteDestinationPath + File.separator + thumbFilename);

		// Does the file already exist?
		if (Files.exists(destinationFile)) {
			try {
				Files.delete(destinationFile);
			} catch (IOException e) {
				log.error("Failed to delete thumb file: {}", destinationFile);
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, RESPONSE_STATUS_EXCEPTION_MESSAGE);
			}
		} else {
			createAndVerifyDirectory(absoluteDestinationPath);
		}

		// Generate thumb, for now we only handle images
		if (fileClassEnum.equals(FileClassEnum.IMAGE)) {
			imageTools.resizeImage(sourceFile, destinationFile, (thumbnailSize != 0 ? thumbnailSize : 250), 0.5F);
		} else {
			log.info("Unsupported file class {}", fileClassEnum.shortName);
		}

		var dimensions = imageTools.getImageDimensions(destinationFile);
		long filesize = getFileSize(destinationFile);
		var sha1sum = computeSha256(sourceFile.toFile());

		// We store the full path name from the converted directory
		var thumbDestinationFilename = sourceFile.getFileName()
												 .toString();
		var sourceFileExtention = thumbDestinationFilename.substring(thumbDestinationFilename.lastIndexOf("."));
		thumbDestinationFilename = thumbDestinationFilename.replace(sourceFileExtention, "." + imageFormat);

		FileThumb fileThumb;

		// Check if the thumb file already exists, if then we delete the file from the filesystem
		var existingfileThumb = fileThumbPageableRepository.findFileThumbByParentId(commonId);

		if (existingfileThumb.isPresent()) {
			fileThumb = existingfileThumb.get();
			fileThumb.setFilepath(relativeDestinationPath.toString());
			fileThumb.setFilename(thumbDestinationFilename);
			fileThumb.setFilesize(filesize);
			fileThumb.setHeight(dimensions.height);
			fileThumb.setWidth(dimensions.width);
			fileThumb.setSha1sum(sha1sum);
		} else {
			fileThumb = FileThumb.builder()
								 .filepath(relativeDestinationPath.toString())
								 .filename(thumbDestinationFilename)
								 .filesize(filesize)
								 .parentId(commonId)
								 .parentClass(fileClassEnum)
								 .height(dimensions.height)
								 .width(dimensions.width)
								 .sha1sum(sha1sum)
								 .build();
		}

		fileThumbPageableRepository.save(fileThumb);
	}

	public void delete(long id) {
		fileThumbPageableRepository.deleteById(id);
	}

	public void deleteByParentId(long parentId) {
		fileThumbPageableRepository.deleteByParentId(parentId);
	}
}
