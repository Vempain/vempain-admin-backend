package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.FileClassEnum;
import fi.poltsi.vempain.admin.api.PublishResultEnum;
import fi.poltsi.vempain.admin.api.request.FileProcessRequest;
import fi.poltsi.vempain.admin.api.response.RefreshDetailResponse;
import fi.poltsi.vempain.admin.api.response.RefreshResponse;
import fi.poltsi.vempain.admin.api.response.file.FileAudioResponse;
import fi.poltsi.vempain.admin.api.response.file.FileCommonResponse;
import fi.poltsi.vempain.admin.api.response.file.FileDocumentResponse;
import fi.poltsi.vempain.admin.api.response.file.FileImageResponse;
import fi.poltsi.vempain.admin.api.response.file.FileVideoResponse;
import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.admin.entity.file.AbstractFileEntity;
import fi.poltsi.vempain.admin.entity.file.FileAudio;
import fi.poltsi.vempain.admin.entity.file.FileCommon;
import fi.poltsi.vempain.admin.entity.file.FileDocument;
import fi.poltsi.vempain.admin.entity.file.FileImage;
import fi.poltsi.vempain.admin.entity.file.FileThumb;
import fi.poltsi.vempain.admin.entity.file.FileVideo;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.entity.file.GalleryFile;
import fi.poltsi.vempain.admin.exception.VempainAclException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.repository.file.FileAudioPageableRepository;
import fi.poltsi.vempain.admin.repository.file.FileCommonPageableRepository;
import fi.poltsi.vempain.admin.repository.file.FileDocumentPageableRepository;
import fi.poltsi.vempain.admin.repository.file.FileImagePageableRepository;
import fi.poltsi.vempain.admin.repository.file.FileThumbPageableRepository;
import fi.poltsi.vempain.admin.repository.file.FileVideoPageableRepository;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.repository.file.SubjectRepository;
import fi.poltsi.vempain.admin.service.AbstractService;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.admin.service.AclService;
import fi.poltsi.vempain.admin.service.PageGalleryService;
import fi.poltsi.vempain.admin.service.PageService;
import fi.poltsi.vempain.admin.service.SubjectService;
import fi.poltsi.vempain.tools.ImageTools;
import fi.poltsi.vempain.tools.MetadataTools;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static fi.poltsi.vempain.tools.AudioTools.getAudioLength;
import static fi.poltsi.vempain.tools.JsonTools.extractMimetype;
import static fi.poltsi.vempain.tools.JsonTools.getDescriptionFromJson;
import static fi.poltsi.vempain.tools.JsonTools.getOriginalDateTimeFromJson;
import static fi.poltsi.vempain.tools.JsonTools.getOriginalDocumentId;
import static fi.poltsi.vempain.tools.JsonTools.getOriginalSecondFraction;
import static fi.poltsi.vempain.tools.JsonTools.getSubjects;
import static fi.poltsi.vempain.tools.LocalFileTools.createAndVerifyDirectory;
import static fi.poltsi.vempain.tools.LocalFileTools.getFileSize;
import static fi.poltsi.vempain.tools.LocalFileTools.getSha1OfFile;
import static fi.poltsi.vempain.tools.LocalFileTools.setExtension;
import static fi.poltsi.vempain.tools.TemplateTools.processTemplateFile;
import static fi.poltsi.vempain.tools.VideoTools.getVideoDimensions;
import static fi.poltsi.vempain.tools.VideoTools.getVideoLength;

@Slf4j
@Service
public class FileService extends AbstractService {

	private final FileCommonPageableRepository   fileCommonPageableRepository;
	private final FileImagePageableRepository    fileImagePageableRepository;
	private final FileAudioPageableRepository    fileAudioPageableRepository;
	private final FileThumbPageableRepository    fileThumbPageableRepository;
	private final FileDocumentPageableRepository fileDocumentPageableRepository;
	private final FileVideoPageableRepository    fileVideoPageableRepository;
	private final GalleryRepository              galleryRepository;
	private final GalleryFileService             galleryFileService;
	private final SubjectRepository              subjectRepository;
	private final FileThumbService               fileThumbService;
	private final SubjectService                 subjectService;
	private final PageService                    pageService;
	private final PageGalleryService			 pageGalleryService;

	@Autowired
	private MetadataTools metadataTools;
	@Autowired
	private ImageTools imageTools;

	@Value("${vempain.admin.file.converted-directory}")
	private String convertedDirectory;
	@Value("${vempain.admin.file.image-format}")
	private String imageFormat;

	private static final String RESPONSE_STATUS_EXCEPTION_MESSAGE = "Unknown error";

	@Autowired
	public FileService(AclService aclService, FileCommonPageableRepository fileCommonPageableRepository,
					   FileImagePageableRepository fileImagePageableRepository,
					   FileAudioPageableRepository fileAudioPageableRepository,
					   FileDocumentPageableRepository fileDocumentPageableRepository,
					   FileVideoPageableRepository fileVideoPageableRepository,
					   FileThumbPageableRepository fileThumbPageableRepository,
					   GalleryRepository galleryRepository, GalleryFileService galleryFileService,
					   AccessService accessService, SubjectRepository subjectRepository,
					   FileThumbService fileThumbService, SubjectService subjectService,
					   PageService pageService, PageGalleryService pageGalleryService) {
		super(aclService, accessService);
		this.fileCommonPageableRepository   = fileCommonPageableRepository;
		this.fileImagePageableRepository    = fileImagePageableRepository;
		this.fileAudioPageableRepository    = fileAudioPageableRepository;
		this.fileDocumentPageableRepository = fileDocumentPageableRepository;
		this.fileVideoPageableRepository    = fileVideoPageableRepository;
		this.fileThumbPageableRepository    = fileThumbPageableRepository;
		this.galleryRepository              = galleryRepository;
		this.galleryFileService             = galleryFileService;
		this.subjectRepository              = subjectRepository;
		this.fileThumbService               = fileThumbService;
		this.subjectService                 = subjectService;
		this.pageService                    = pageService;
		this.pageGalleryService             = pageGalleryService;
	}

	@PostConstruct
	public void setupEnv() {
		var currentPath = System.getProperty("user.dir");
		log.info("Current directory: {}", currentPath);
		var exceptionMessage = "Unable to initiate the main storage directory";
		var convertedPath    = Path.of(convertedDirectory);

		if (!convertedPath.toFile().exists()) {
			try {
				createAndVerifyDirectory(convertedPath);
			} catch (Exception e) {
				log.error("Could not create converted main file storage: {}", convertedPath);
				throw new FileSystemNotFoundException(exceptionMessage);
			}
		}

		if (!Files.isReadable(convertedPath)) {
			log.error("Converted main file storage exists  but it has wrong permission: {}", convertedPath);
			throw new FileSystemNotFoundException(exceptionMessage);
		}
	}

	// FileGallery
	public List<Gallery> findAllGalleries() {
		return galleryRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
	}

	public Gallery findGalleryById(Long galleryId) {
		var optionalGallery = galleryRepository.findById(galleryId);

		if (optionalGallery.isEmpty()) {
			return null;
		}

		var gallery         = optionalGallery.get();
		var galleryFiles    = galleryFileService.findGalleryFileByGalleryId(galleryId);
		var commonFileIdSet = new ArrayList<Long>();

		for (GalleryFile galleryFile : galleryFiles) {
			commonFileIdSet.add(galleryFile.getFileCommonId());
		}

		var commonFiles = fileCommonPageableRepository.findByIdIn(commonFileIdSet);

		gallery.setCommonFiles(commonFiles);
		return gallery;
	}

	public Gallery findGalleryByShortname(String galleryName) {
		var optionalGallery = galleryRepository.findByShortname(galleryName);

		return optionalGallery.map(gallery -> findGalleryById(gallery.getId()))
							  .orElse(null);
	}

	public Gallery createGallery(String shortName, String description, long userId, List<FileCommon> fileCommonList) {
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
								 .creator(userId)
								 .created(Instant.now())
								 .aclId(aclId)
								 .locked(false)
								 .build();
		var newGallery = galleryRepository.save(gallery);

		long sortOrder = 0;

		for (FileCommon fileCommon : fileCommonList) {
			galleryFileService.addGalleryFile(newGallery.getId(), fileCommon.getId(), sortOrder);
			sortOrder++;
		}

		return gallery;
	}

	public Gallery createEmptyGallery(String shortName, String description, Long userId) {
		return createGallery(shortName, description, userId, new ArrayList<>());
	}

	public void saveGallery(Gallery gallery) {
		galleryRepository.save(gallery);
	}

	// FileCommon
	public Iterable<FileCommon> findAllFileCommon() {
		return fileCommonPageableRepository.findAll();
	}

	public List<FileCommonResponse> findAllCommonAsResponseForUser(int pageNumber, int pageSize) {
		Pageable pageable    = PageRequest.of(pageNumber, pageSize);
		var      commonFiles = fileCommonPageableRepository.findAll(pageable);
		log.info("Found {} common files", commonFiles.stream()
													 .count());
		ArrayList<FileCommonResponse> fileCommonResponses = new ArrayList<>();

		for (FileCommon fileCommon : commonFiles) {
			log.info("Checking common file {} with ACL ID: {}", fileCommon.getConvertedFile(), fileCommon.getAclId());

			if (accessService.hasReadPermission(fileCommon.getAclId())) {
				log.info("Access ok for {}", fileCommon.getConvertedFile());
				fileCommonResponses.add(fileCommon.toResponse());
			}
		}

		log.info("Returning response list:\n{}", fileCommonResponses);
		return fileCommonResponses;
	}

	public Iterable<FileCommon> findAllPageableFileCommon(int pageNumber, int pageSize) {
		Pageable pageable = PageRequest.of(pageNumber, pageSize);
		return fileCommonPageableRepository.findAll(pageable);
	}

	public Optional<FileCommon> findCommonById(long commonId) {
		return fileCommonPageableRepository.findById(commonId);
	}

	public List<FileCommon> findAllFileCommonWithGalleryFileList(List<GalleryFile> galleryFileList) {
		var fileCommonList = new ArrayList<FileCommon>();

		for (GalleryFile galleryFile : galleryFileList) {
			var optionalCommonFile = findCommonById(galleryFile.getFileCommonId());

			if (optionalCommonFile.isPresent()) {
				fileCommonList.add(optionalCommonFile.get());
			} else {
				log.error("Could not find FileCommon {} referred by gallery ID {}", galleryFile.getFileCommonId(), galleryFile.getGalleryId());
			}
		}

		return fileCommonList;
	}

	public FileCommon saveFileCommon(FileCommon fileCommon) {
		return fileCommonPageableRepository.save(fileCommon);
	}

	public Set<Long> findAllFileCommonIdWithSubject() {
		var fileCommonIds = fileCommonPageableRepository.getAllFileCommonWithSubjects();

		return new HashSet<>(fileCommonIds);
	}


	/**
	 * Process uploaded files storing them in the file storage, and if defined, create a gallery for these files
	 *
	 * @param multipartFiles     Uploaded file
	 * @param galleryShortname   Short name for gallery
	 * @param galleryDescription Description of gallery
	 */
	public void processUploadFiles(MultipartFile[] multipartFiles, String destinationDirectory, String galleryShortname,
								   String galleryDescription) {
		var  usedId    = getUserId();
		Long galleryId = null;

		if (galleryShortname != null &&
			galleryDescription != null) {
			var gallery = createEmptyGallery(galleryShortname, galleryDescription, usedId);
			galleryId = gallery.getId();
		}

		var sortOrder = 0L;
		for (MultipartFile multipartFile : multipartFiles) {
			if (!multipartFile.isEmpty()) {
				try {
					processUploadFile(multipartFile.getInputStream(), multipartFile.getOriginalFilename(), destinationDirectory,
									  usedId, galleryId, sortOrder);
				} catch (IOException e) {
					log.error("Failed to process file: {}", multipartFile.getOriginalFilename(), e);
					throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, RESPONSE_STATUS_EXCEPTION_MESSAGE);
				}
			} else {
				log.error("Got an empty upload file: {}", multipartFile.getOriginalFilename());
			}

			sortOrder++;
		}
	}

	private void processUploadFile(InputStream inputStream, String originalFilename, String destinationDirectory,
								   long userId, Long galleryId, long sortOrder) {
		File sourceFile = new File(File.separator + "tmp" + File.separator + originalFilename);
		try (OutputStream output = new FileOutputStream(sourceFile, false)) {
			inputStream.transferTo(output);
		} catch (FileNotFoundException e) {
			log.error("Failed to create file: {}", originalFilename, e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, RESPONSE_STATUS_EXCEPTION_MESSAGE);
		} catch (IOException e) {
			log.error("Failed to save file: {}", originalFilename, e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, RESPONSE_STATUS_EXCEPTION_MESSAGE);
		}

		processCommonFile(sourceFile.toPath(), destinationDirectory, userId, galleryId, sortOrder);
	}


	/**
	 * Get all files in a given directory and add them to database. The directory must exist as a subdirectory of the converted-directory
	 *
	 * @param fileProcessRequest File migration request from the client
	 * @throws VempainEntityNotFoundException If path does not exist
	 *                                        return List of FileCommon objects
	 */

	public List<FileCommon> addFilesFromDirectory(FileProcessRequest fileProcessRequest, long userId) throws VempainEntityNotFoundException,
																								IOException, VempainAclException {
		// String sourceDirectory, String siteDirectory, boolean createGallery,
		// String galleryName, String galleryDescription
		var sourcePath = Path.of(convertedDirectory + File.separator + fileProcessRequest.getSourceDirectory());

		log.info("Adding files from directory: {}", sourcePath);

		if (!Files.exists(sourcePath)) {
			log.error("Given path '{}' does not exist for file addition. The work directory is: {}", sourcePath, System.getProperty("user.dir"));
			throw new VempainEntityNotFoundException("Could not find path to add files from", "filecommon");
		}

		final Long galleryId = fileProcessRequest.isGenerateGallery() ? createEmptyGallery(fileProcessRequest.getGalleryShortname(),
																						   fileProcessRequest.getGalleryDescription(), userId).getId() : null;

		var fileList = new ArrayList<Path>();

		try (Stream<Path> files = Files.walk(sourcePath).filter(Files::isRegularFile)) {
			files.forEach(fileList::add);
		}

		if (fileProcessRequest.isGeneratePage()) {
			var pageAclId    = aclService.createNewAcl(userId, null, true, true, true, true);
			var galleryIdMap = Map.of("galleryId", String.valueOf(galleryId));
			var completePage = fileProcessRequest.getPageBody() + "\n" + processTemplateFile("templates/gallery_template.php", galleryIdMap);
			var newPage = fi.poltsi.vempain.admin.entity.Page.builder()
															 .title(fileProcessRequest.getPageTitle())
															 .header(fileProcessRequest.getPageTitle())
															 .path(fileProcessRequest.getPagePath())
															 .body(completePage)
															 .formId(fileProcessRequest.getPageFormId())
															 .aclId(pageAclId)
															 .creator(userId)
															 .created(Instant.now())
															 .build();
			var page = pageService.save(newPage);
			pageGalleryService.addPageGallery(page.getId(), galleryId, 0);
		}

		return addFilesToDatabase(fileProcessRequest.getDestinationDirectory(), fileList, userId, galleryId);
	}

	public ArrayList<FileCommon> addFilesToDatabase(String siteDirectory, List<Path> fileList, Long userId, Long galleryId) {
		var sortOrder      = 0L;
		var commonFileList = new ArrayList<FileCommon>();
		var relativizer    = Path.of(convertedDirectory + File.separator);

		for (Path path : fileList) {
			var relativePath = relativizer.relativize(path);
			log.info("Adding relative path: {}", relativePath);
			commonFileList.add(processCommonFile(relativePath, siteDirectory, userId, galleryId, sortOrder));
			sortOrder++;
		}

		return commonFileList;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	protected FileCommon processCommonFile(Path relativeSourceFile, String siteDirectory, Long userId, Long galleryId, long sortOrder) {
		// Check first if the file already exists in the database
		var alreadyExists = fileCommonPageableRepository.findByConvertedFile(relativeSourceFile.toString());

		if (alreadyExists.isPresent()) {
			log.info("File {} already exists in the database", relativeSourceFile);
			return alreadyExists.get();
		}

		var absolutePath = Path.of(convertedDirectory + File.separator + relativeSourceFile);
		// Get sha1sum
		var sourceSha1Sum = getSha1OfFile(absolutePath.toFile());

		if (sourceSha1Sum == null || sourceSha1Sum.isBlank()) {
			log.error("Failed to get sha1sum from source file: {}", absolutePath);
			return null;
		}

		log.debug("Checksum of source file: {}", sourceSha1Sum);

		// Get metadata
		var metadata = metadataTools.getMetadataAsJSON(absolutePath.toFile());

		if (metadata == null || metadata.isEmpty()) {
			log.error("Failed to get metadata from file: {}", absolutePath);
			return null;
		}

		var jsonObject = metadataToJsonObject(metadata);

		if (jsonObject == null) {
			return null;
		}

		log.debug("Extracted JSON object\n{}", jsonObject);
		// Extract comment from metadata, it may not exist
		var description = getDescriptionFromJson(jsonObject);

		var originalDateTimeString = getOriginalDateTimeFromJson(jsonObject);
		var originalDateTime       = dateTimeParser(originalDateTimeString);
		var originalSecondFraction = getOriginalSecondFraction(jsonObject);
		var originalDocumentId     = getOriginalDocumentId(jsonObject);

		// Get filesize
		var sourceFileSize = getFileSize(absolutePath);

		var mimetype = extractMimetype(jsonObject);

		if (mimetype == null) {
			log.warn("Failed to extract mimetype from JSON: {}", jsonObject);
		} else {
			log.debug("Mimetype: {}", mimetype);
		}

		// We need to first create the common file so that we get the id
		Long aclId;
		try {
			aclId = aclService.createNewAcl(userId, null, true, true, true, true);
		} catch (VempainAclException e) {
			log.error("Storing ACL list failed for unknown reason: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, RESPONSE_STATUS_EXCEPTION_MESSAGE);
		}

		// In case of image file, the destination file is always stored in the imageFormat-format, so the filename is not necessarily
		// that of the original. The extracted metadata is that from the original file, which we let pass at this point
		var destinationFile = absolutePath.getFileName()
										  .toString();

		if (FileClassEnum.getFileClassByMimetype(mimetype)
						 .equals(FileClassEnum.IMAGE)) {
			destinationFile = setExtension(absolutePath.getFileName()
													   .toString(), imageFormat);
		}

		var fileCommon = FileCommon.builder()
								   // Source
								   .convertedFile(relativeSourceFile.toString())
								   .convertedFilesize(sourceFileSize)
								   .convertedSha1sum(sourceSha1Sum)
								   // Original extracted
								   .originalDatetime(originalDateTime)
								   .originalSecondFraction(originalSecondFraction)
								   .originalDocumentId(originalDocumentId)
								   // Site
								   .siteFilepath(siteDirectory)
								   .siteFilename(destinationFile)
								   .comment(description)
								   .mimetype(mimetype)
								   .metadata(metadata)
								   .fileClassId(FileClassEnum.getFileClassIdByMimetype(mimetype))
								   .locked(false)
								   .aclId(aclId)
								   .creator(userId)
								   .created(Instant.now())
								   .modified(null)
								   .modifier(null)
								   .build();
		fileCommon = fileCommonPageableRepository.save(fileCommon);

		// Then we store the type specific data, depending of the FileClassEnum
		switch (FileClassEnum.getFileClassByMimetype(mimetype)) {
			case AUDIO -> {
				// Get the length of the audio file
				var length = getAudioLength(absolutePath.toFile());
				var fileAudio = FileAudio.builder()
										 .parentId(fileCommon.getId())
										 .length(length)
										 .build();
				fileAudioPageableRepository.save(fileAudio);
			}
			case DOCUMENT -> {
				// TODO create a document tool which can get the document specific characteristics
				var fileDocument = FileDocument.builder()
											   .parentId(fileCommon.getId())
											   .pages(0L)
											   .build();
				fileDocumentPageableRepository.save(fileDocument);
			}
			case IMAGE -> {
				var dimensions = imageTools.getImageDimensions(absolutePath);
				var fileImage = FileImage.builder()
										 .parentId(fileCommon.getId())
										 .height(dimensions.height)
										 .width(dimensions.width)
										 .build();
				fileImagePageableRepository.save(fileImage);
			}
			case VIDEO -> {
				var videoLength    = getVideoLength(jsonObject);
				var videDimensions = getVideoDimensions(jsonObject);
				var fileVideo = FileVideo.builder()
										 .parentId(fileCommon.getId())
										 .height(videDimensions.height)
										 .width(videDimensions.width)
										 .length(videoLength)
										 .build();
				fileVideoPageableRepository.save(fileVideo);
			}
			default -> log.error("Unknown file class: {}", FileClassEnum.getFileClassByMimetype(mimetype));
		}

		// Get the list of ACLs
		var acls = aclService.findAclByAclId(aclId);
		fileCommon.setAcls(acls);
		// Extract subjects from metadata XMP-section
		extractAndStoreSubjects(fileCommon.getId(), jsonObject);

		// Handle gallery
		if (galleryId != null) {
			galleryFileService.addGalleryFile(galleryId, fileCommon.getId(), sortOrder);
		}

		fileThumbService.generateThumbFile(fileCommon.getId());

		return fileCommon;
	}

	private static JSONObject metadataToJsonObject(String metadata) {
		var jsonArray = new JSONArray(metadata);

		if (jsonArray.isEmpty()) {
			log.error("Failed to parse the metadata JSON from\n{}", metadata);
			return null;
		}

		var jsonObject = jsonArray.getJSONObject(0);
		return jsonObject;
	}

	private Instant dateTimeParser(String dateTimeString) {
		if (dateTimeString == null || dateTimeString.isBlank()) {
			return null;
		}

		DateTimeFormatter formatter = new DateTimeFormatterBuilder()
				// First, try parsing with the pattern that includes offset
				.appendOptional(DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ssXXX"))
				// If not successful, try parsing without offset
				.appendOptional(DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"))
				.toFormatter()
				.withZone(ZoneId.systemDefault());
		return Instant.from(formatter.parse(dateTimeString, Instant::from));
	}

	@Transactional(propagation = Propagation.REQUIRED)
	protected void extractAndStoreSubjects(long commonFileId, JSONObject jsonObject) {
		List<String> subjectList = getSubjects(jsonObject);
		saveSubjectList(commonFileId, subjectList);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	protected void saveSubjectList(long commonFileId, List<String> subjectList) {
		for (String subjectName : subjectList) {
			var optionalSubject = findSubjectBySubjectNameAndLanguage(subjectName, "");

			if (optionalSubject.isPresent()) {
				saveFileSubject(optionalSubject.get()
											   .getId(), commonFileId);
			} else {
				log.info("Subject {} does not exist, creating it", subjectName);
				// Create the subject and save it
				var subject = Subject.builder()
									 .subjectName(subjectName)
									 .build();
				var newSubject = saveSubject(subject);

				saveFileSubject(newSubject.getId(), commonFileId);
			}
		}
	}

	public void deleteFile(long id) {
		// All the auxiliary data (thumb, image, document etc) should be deleted as CASCADE of deleting the FileCommon row
		// Remove the thumb file from filesystem
		var thumbFile = fileThumbPageableRepository.getFilePathByParentId(id);

		if (thumbFile != null) {
			try {
				Files.delete(Paths.get(thumbFile));
			} catch (IOException e) {
				log.error("Failed to remove thumb file: {}", thumbFile, e);
			}
		} else {
			log.warn("Could not find a thumb file for file ID {}", id);
		}
		// Remove the actual file from filesystem
		var fileCommonFile = fileCommonPageableRepository.getFilePathByParentId(id);

		if (fileCommonFile != null) {
			try {
				Files.delete(Paths.get(fileCommonFile));
			} catch (IOException e) {
				log.error("Failed to remove file: {}", fileCommonFile, e);
			}
		}

		// Finally remove the FileCommon entry from database which should cascade everywhere
		fileCommonPageableRepository.deleteById(id);
	}

	// FileSubject
	public void removeFileSubjects(Set<Long> fileSubjectIdSet) {
		fileCommonPageableRepository.deleteAllBySubjectId(fileSubjectIdSet);
	}

	public List<Subject> findAllSubjectsByFileCommonId(Long fileCommonId) {
		return subjectRepository.getSubjectsByFileId(fileCommonId);
	}

	public List<FileCommon> findAllFilesBySubject(String subjectName) {
		var optionalSubject = subjectRepository.findSubjectBySubjectName(subjectName);

		if (optionalSubject.isEmpty()) {

			for (String lang : Arrays.asList("en", "fi", "de", "se")) {
				optionalSubject = findSubjectBySubjectNameAndLanguage(subjectName, lang);

				if (optionalSubject.isPresent()) {
					break;
				}
			}
		}

		var fileCommons = new ArrayList<FileCommon>();

		if (optionalSubject.isEmpty()) {
			return fileCommons;
		}

		return fileCommonPageableRepository.getFileCommonBySubjectId(optionalSubject.get().getId());
	}

	public Optional<Subject> findSubjectBySubjectNameAndLanguage(String subjectName, String language) {
		return switch (language) {
			case "en" -> subjectRepository.findSubjectBySubjectNameEn(subjectName);
			case "fi" -> subjectRepository.findSubjectBySubjectNameFi(subjectName);
			case "de" -> subjectRepository.findSubjectBySubjectNameDe(subjectName);
			case "se" -> subjectRepository.findSubjectBySubjectNameSe(subjectName);
			default -> subjectRepository.findSubjectBySubjectName(subjectName);
		};
	}

	private void saveFileSubject(Long subjectId, Long fileCommonId) {
		log.debug("Adding subject ID {} to fileCommon ID {}, checking if they exist", subjectId, fileCommonId);

		var optionalFileCommon = fileCommonPageableRepository.findById(fileCommonId);

		if (optionalFileCommon.isPresent()) {
			log.debug("File common ID {} exists", optionalFileCommon.get().getId());

			var optionalSubject = subjectRepository.findById(subjectId);

			if (optionalSubject.isPresent()) {
				log.debug("Subject ID {} exists", optionalSubject.get().getId());
				subjectService.addSubjectToFile(subjectId, fileCommonId);
			} else {
				log.error("Subject ID {} does not exist", subjectId);
			}
		} else {
			log.error("File common ID {} does not exist", fileCommonId);
		}
	}

	// FileAudio
	public Page<FileAudioResponse> findAllAudiosAsPageableResponseFiltered(PageRequest pageRequest, String filter, String filterColumn) {
		Page<FileAudio> fileAudios;

		if (filter == null || filter.isBlank()
			|| filterColumn == null || filterColumn.isBlank()) {
			fileAudios = fileAudioPageableRepository.findAll(pageRequest);
		} else {
			fileAudios = switch (filterColumn) {
				case "length" -> fileAudioPageableRepository.findByLengthContaining(Long.getLong(filter), pageRequest);
				default -> fileAudioPageableRepository.findAll(pageRequest);
			};
		}

		populateFileEntityPage(fileAudios);
		return fileAudios.map(FileAudio::toResponse);
	}

	// FileDocument
	public Page<FileDocumentResponse> findAllDocumentsAsPageableResponseFiltered(PageRequest pageRequest, String filter,
																				 String filterColumn) {
		Page<FileDocument> fileDocuments;

		if (filter == null || filter.isBlank()
			|| filterColumn == null || filterColumn.isBlank()) {
			fileDocuments = fileDocumentPageableRepository.findAll(pageRequest);
		} else {
			fileDocuments = switch (filterColumn) {
				case "length" -> fileDocumentPageableRepository.findByPagesContaining(Long.getLong(filter), pageRequest);
				default -> fileDocumentPageableRepository.findAll(pageRequest);
			};
		}

		populateFileEntityPage(fileDocuments);
		return fileDocuments.map(FileDocument::toResponse);
	}

	// FileImage
	public Page<FileImageResponse> findAllImagesAsPageableResponseFiltered(PageRequest pageRequest, String filter, String filterColumn) {
		Page<FileImage> fileImages;

		if (filter == null || filter.isBlank()
			|| filterColumn == null || filterColumn.isBlank()) {
			fileImages = fileImagePageableRepository.findAll(pageRequest);
		} else {
			fileImages = switch (filterColumn) {
				case "height" -> fileImagePageableRepository.findByHeightContaining(Long.getLong(filter), pageRequest);
				case "parent_id" -> fileImagePageableRepository.findByParentIdContaining(Long.getLong(filter), pageRequest);
				case "width" -> fileImagePageableRepository.findByWidthContaining(Long.getLong(filter), pageRequest);
				default -> fileImagePageableRepository.findAll(pageRequest);
			};
		}

		populateFileEntityPage(fileImages);
		return fileImages.map(FileImage::toResponse);
	}

	public Iterable<FileImage> findAllFileImagesByFilepathFilename(String siteFilePath, String siteFilename) {
		return fileImagePageableRepository.findAllFileImageByFileCommonPathAndName(siteFilePath, siteFilename);
	}

	public FileImageResponse findImageById(long parentId) {
		var optionalImage = fileImagePageableRepository.findById(parentId);

		if (optionalImage.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND);
		}

		var image = optionalImage.get();

		populateFileEntity(image);

		return image.toResponse();
	}

	public Iterable<FileImage> findAllImagesWithoutThumbnail() {
		return fileImagePageableRepository.findAllFileImageWithoutThumbnail();
	}

	public void deleteFileImage(FileImage fileImage) {
		fileImagePageableRepository.delete(fileImage);
	}

	// FileVideo
	public Page<FileVideoResponse> findAllVideosAsPageableResponseFiltered(PageRequest pageRequest, String filter, String filterColumn) {
		Page<FileVideo> fileVideos;

		if (filter == null || filter.isBlank()
			|| filterColumn == null || filterColumn.isBlank()) {
			fileVideos = fileVideoPageableRepository.findAll(pageRequest);
		} else {
			fileVideos = switch (filterColumn) {
				case "length" -> fileVideoPageableRepository.findByLengthContaining(Long.getLong(filter), pageRequest);
				default -> fileVideoPageableRepository.findAll(pageRequest);
			};
		}

		populateFileEntityPage(fileVideos);
		return fileVideos.map(FileVideo::toResponse);
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
		var duplicates   = new ArrayList<FileThumb>();

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

	public List<FileThumb> findAllFileThumbsByFileCommonList(List<FileCommon> fileCommonList) {
		var thumbList = new ArrayList<FileThumb>();

		for (var fileCommon : fileCommonList) {
			var optionalFileThumb = fileThumbPageableRepository.findFileThumbByParentId(fileCommon.getId());

			if (optionalFileThumb.isEmpty()) {
				log.error("FileCommon ID {} is missing a thumb file", fileCommon.getId());
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

	// Responsive features
	public List<String> getConvertedDirectories() {
		var directories = new ArrayList<String>();

		try (Stream<Path> entries = Files.walk(Paths.get(convertedDirectory), Integer.MAX_VALUE)
										 .filter(Files::isDirectory)
										 .onClose(() -> log.info("Stream closed"))) {
			entries.forEach(x -> addConvertedPathToList(directories, x.toAbsolutePath()));
			return directories;
		} catch (IOException e) {
			log.error("Failed to read the main import directory: {}", convertedDirectory);
			throw new FileSystemNotFoundException("Failed to read the import directory");
		}
	}

	public List<String> matchConvertedDirectories(String matchPath) {
		var directories = new ArrayList<String>();
		// First check if we have any path separators
		if (matchPath.contains(File.separator)) {
			var parentDir = matchPath.substring(0, matchPath.lastIndexOf(File.separator));
			log.info("Matching path {} has a separator ({}) and the parent is {}", matchPath, File.separator, parentDir);
			var parentPath = Paths.get(convertedDirectory + parentDir);

			if (Files.exists(parentPath) && parentPath.toFile()
													  .isDirectory()) {
				var subDir = matchPath.substring(matchPath.lastIndexOf(File.separator) + 1);
				log.info("Parent path {} does exist, continuing", parentPath);
				return matchSubDirectories(convertedDirectory + File.separator + parentDir, subDir);
			} else {
				log.warn("Parent path {} does not exist", parentPath);
				return directories;
			}
		} else {
			log.info("Search path does not contain any directory separators, matching only dirs in current directory ({})",
					 convertedDirectory + File.separator + matchPath);
			return matchSubDirectories(convertedDirectory, matchPath);
		}
	}

	private List<String> matchSubDirectories(String directory, String matchPath) {
		log.info("matchSubDirectories: Called with directory {} and matchPath {}", directory, matchPath);
		var directories = new ArrayList<String>();

		try (Stream<Path> entries = Files.walk(Paths.get(directory + File.separator + matchPath), 1)
										 .filter(Files::isDirectory)
										 .onClose(() -> log.info("Stream closed"))) {
			entries.forEach(x -> addConvertedPathToList(directories, x.toAbsolutePath()));
		} catch (IOException e) {
			log.error("Failed to correctly filter directory {} for match {}", directory, matchPath, e);
			throw new FileSystemNotFoundException("Failed to read the import directory");
		}

		return directories;
	}

	private void addConvertedPathToList(List<String> pathList, Path newPath) {
		log.debug("Removing {} from {}", convertedDirectory, newPath.toString());
		var relativePath = newPath.toString()
								  .replaceFirst(convertedDirectory, "");

		// If the length of the relative path now is 0, then skip adding it to the list
		if (relativePath.isEmpty()) {
			log.debug("Skipping empty path after removing main import directory: {}", newPath);
			return;
		}

		log.debug("Adding path to list: {}", relativePath);
		pathList.add(relativePath);
	}

	private <T extends AbstractFileEntity> void populateFileEntityPage(Page<T> fileEntities) {
		for (var fileEntity : fileEntities) {
			populateFileEntity(fileEntity);
		}
	}

	private <T extends AbstractFileEntity> void populateFileEntity(T fileEntity) {
		var optionalFileCommon = fileCommonPageableRepository.findById(fileEntity.getParentId());

		if (optionalFileCommon.isPresent()) {
			fileEntity.setFileCommon(optionalFileCommon.get());

			var acls = aclService.findAclByAclId(optionalFileCommon.get()
																   .getAclId());
			fileEntity.getFileCommon()
					  .setAcls(acls);
		} else {
			log.error("Failed to find the common file part of the file image with parent ID: {}", fileEntity.getParentId());
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public RefreshResponse refreshAllGalleryFiles() {
		var refreshResponse = RefreshResponse.builder()
											 .details(new ArrayList<>())
											 .build();
		var successCount = 0L;
		var failedCount  = 0L;
		// First get all the gallery IDs
		var galleryIds = galleryRepository.getAllGalleryIds();
		for (Long galleryId : galleryIds) {
			var galleryResponse = refreshGalleryFiles(galleryId);
			log.debug("Gallery {} refresh result: {}", galleryId, galleryResponse);

			successCount = +galleryResponse.getRefreshedItems();
			failedCount  = +galleryResponse.getFailedItems();
			refreshResponse.getDetails().addAll(galleryResponse.getDetails());
		}

		refreshResponse.setRefreshedItems(successCount);
		refreshResponse.setFailedItems(failedCount);
		refreshResponse.setResult(failedCount == 0 ? PublishResultEnum.OK : PublishResultEnum.FAIL);
		return refreshResponse;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public RefreshResponse refreshGalleryFiles(long galleryId) {
		var galleryFiles   = galleryFileService.findGalleryFileByGalleryId(galleryId);
		var refreshDetails = new ArrayList<RefreshDetailResponse>();
		var successCount   = 0;
		var failedCount    = 0;

		for (var galleryFile : galleryFiles) {
			log.debug("Refreshing gallery file: {}", galleryFile);
			var refreshDetail = refreshFile(galleryFile.getFileCommonId());
			refreshDetails.add(refreshDetail);

			if (refreshDetail != null && refreshDetail.getResult() == PublishResultEnum.OK) {
				successCount++;
			} else {
				failedCount++;
			}
		}

		return RefreshResponse.builder()
							  .refreshedItems(successCount)
							  .failedItems(failedCount)
							  .result(failedCount == 0 ? PublishResultEnum.OK : PublishResultEnum.FAIL)
							  .details(refreshDetails)
							  .build();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	protected RefreshDetailResponse refreshFile(long fileCommonId) {
		// Get the fileCommon object
		var optionalFileCommon = fileCommonPageableRepository.findById(fileCommonId);

		if (optionalFileCommon.isEmpty()) {
			log.error("Could not find fileCommon with ID {}", fileCommonId);
			return null;
		}

		var fileCommon = optionalFileCommon.get();
		var fileClass  = FileClassEnum.getFileClassByMimetype(fileCommon.getMimetype());
		var resultMap  = new HashMap<PublishResultEnum, String>();

		var fileResult = refreshCommonFile(fileCommon, resultMap);

		if (fileResult) {

			switch (fileClass) {
				case IMAGE -> fileResult = refreshImageFile(fileCommon, resultMap);
				case THUMB -> fileResult = refreshThumbFile(fileCommon, resultMap);
				case AUDIO -> fileResult = refreshAudioFile(fileCommon, resultMap);
				case VIDEO -> fileResult = refreshVideoFile(fileCommon, resultMap);
				case DOCUMENT -> fileResult = refreshDocumentFile(fileCommon, resultMap);
				// TODO Implement the rest of the file class refreshes
				default -> {
					log.error("Unhandled refresh file class: {}", fileClass);
					fileResult = false;
				}
			}
		}

		var result = fileResult ? PublishResultEnum.OK : PublishResultEnum.FAIL;

		var description = resultMap.get(result);

		return RefreshDetailResponse.builder()
									.itemId(fileCommonId)
									.itemType(fileClass.shortName)
									.result(result)
									.resultDescription(description)
									.build();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	protected boolean refreshCommonFile(FileCommon fileCommon, HashMap<PublishResultEnum, String> response) {
		var convertedFile = fileCommon.getConvertedFile();

		if (convertedFile == null || convertedFile.isBlank()) {
			response.put(PublishResultEnum.FAIL, "Missing converted file");
			log.error("Missing converted file for fileCommon ID {}", fileCommon.getId());
			return false;
		}

		var convertedFilePath = Path.of(convertedDirectory + File.separator + convertedFile);

		if (!Files.exists(convertedFilePath)) {
			response.put(PublishResultEnum.FAIL, "Converted file does not exist");
			log.error("Converted file does not exist: {}", convertedFilePath);
			return false;
		}

		var sha1sum = getSha1OfFile(convertedFilePath.toFile());

		if (sha1sum == null) {
			response.put(PublishResultEnum.FAIL, "Could not get sha1sum of file");
			log.error("Failed to get sha1sum of file: {}", convertedFilePath);
			return false;
		}

		if (sha1sum.equals(fileCommon.getConvertedSha1sum())) {
			response.put(PublishResultEnum.OK, "File has not changed");
			return true;
		}

		response.put(PublishResultEnum.OK, "File has been updated");
		fileCommon.setConvertedSha1sum(sha1sum);

		// Get the metadata of the file
		var metaTool = new MetadataTools();
		var metadata = metaTool.getMetadataAsJSON(convertedFilePath.toFile());
		fileCommon.setMetadata(metadata);
		// Get the comment from metadata
		var jsonObject = metadataToJsonObject(metadata);

		if (jsonObject == null) {
			response.put(PublishResultEnum.FAIL, "Failed to extract metadata");
			return false;
		}

		var comment                = getDescriptionFromJson(jsonObject);
		var originalDateTimeString = getOriginalDateTimeFromJson(jsonObject);
		var originalDateTime       = dateTimeParser(originalDateTimeString);
		var originalSecondFraction = getOriginalSecondFraction(jsonObject);
		var originalDocumentId     = getOriginalDocumentId(jsonObject);
		var convertedFileSize = getFileSize(convertedFilePath);

		// Remove first old associations between subjects and files
		subjectService.removeAllSubjectsFromFile(fileCommon.getId());
		extractAndStoreSubjects(fileCommon.getId(), jsonObject);

		fileCommon.setComment(comment);
		fileCommon.setOriginalDatetime(originalDateTime);
		fileCommon.setOriginalSecondFraction(originalSecondFraction);
		fileCommon.setOriginalDocumentId(originalDocumentId);
		fileCommon.setConvertedFilesize(convertedFileSize);
		fileCommonPageableRepository.save(fileCommon);

		return true;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	protected boolean refreshImageFile(FileCommon fileCommon, HashMap<PublishResultEnum, String> resultMap) {
		var convertedFile = fileCommon.getConvertedFile();
		var convertedFilePath = Path.of(convertedDirectory + File.separator + convertedFile);

		var dimensions = imageTools.getImageDimensions(convertedFilePath);
		var optionalFileImage = fileImagePageableRepository.findImageFileByParentId(fileCommon.getId());

		var fileImage = optionalFileImage.orElseGet(FileImage::new);

		fileImage.setParentId(fileCommon.getId());
		fileImage.setHeight(dimensions.height);
		fileImage.setWidth(dimensions.width);
		resultMap.put(PublishResultEnum.OK, "Image dimensions updated");

		fileImagePageableRepository.save(fileImage);

		return refreshThumbFile(fileCommon, resultMap);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	protected boolean refreshThumbFile(FileCommon fileCommon, HashMap<PublishResultEnum, String> resultMap) {
		var optionalFileThumb = fileThumbPageableRepository.findFileThumbByParentId(fileCommon.getId());

		if (optionalFileThumb.isPresent()) {
			var thumbFile = optionalFileThumb.get();
			var thumbFilePath = Path.of(convertedDirectory + File.separator + thumbFile.getFilepath() + File.separator + thumbFile.getFilename());

			if (Files.exists(thumbFilePath)) {
				log.debug("Removing existing thumb file: {}", thumbFilePath);

				try {
					Files.delete(thumbFilePath);
				} catch (IOException e) {
					log.error("Failed to remove thumb file: {}", thumbFilePath, e);
					resultMap.put(PublishResultEnum.FAIL, "Failed to remove thumb file");
					return false;
				}
			}

			fileThumbPageableRepository.delete(thumbFile);
			resultMap.put(PublishResultEnum.OK, "Old thumb file removed");
		}

		// Create a new thumb file
		fileThumbService.generateThumbFile(fileCommon.getId());
		var optionalUpdatedThumb = fileThumbPageableRepository.findFileThumbByParentId(fileCommon.getId());

		if (optionalUpdatedThumb.isEmpty()) {
			resultMap.put(PublishResultEnum.FAIL, "Failed to create thumb file");
			return false;
		}

		resultMap.put(PublishResultEnum.OK, "Thumb file created");

		return true;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	protected boolean refreshAudioFile(FileCommon fileCommon, HashMap<PublishResultEnum, String> resultMap) {
		var convertedFilePath = Path.of(convertedDirectory + File.separator + fileCommon.getConvertedFile());
		var audioLength = getAudioLength(convertedFilePath.toFile());

		var optionalAudioFile = fileAudioPageableRepository.findFileAudioByParentId(fileCommon.getId());

		var audioFile = optionalAudioFile.orElseGet(FileAudio::new);
		audioFile.setLength(audioLength);
		audioFile.setParentId(fileCommon.getId());

		fileAudioPageableRepository.save(audioFile);
		resultMap.put(PublishResultEnum.OK, "Audio length updated");

		return false;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	protected boolean refreshVideoFile(FileCommon fileCommon, HashMap<PublishResultEnum, String> resultMap) {
		var jsonObject = metadataToJsonObject(fileCommon.getMetadata());
		var videoLength = getVideoLength(jsonObject);
		var videoDimensions = getVideoDimensions(jsonObject);

		var optionalVideoFile = fileVideoPageableRepository.findFileVideoByParentId(fileCommon.getId());
		var videoFile = optionalVideoFile.orElseGet(FileVideo::new);

		videoFile.setLength(videoLength);
		videoFile.setHeight(videoDimensions.height);
		videoFile.setWidth(videoDimensions.width);
		videoFile.setParentId(fileCommon.getId());

		fileVideoPageableRepository.save(videoFile);
		resultMap.put(PublishResultEnum.OK, "Video length and dimensions updated");

		return true;
	}

	private boolean refreshDocumentFile(FileCommon fileCommon, HashMap<PublishResultEnum, String> resultMap) {
		var optionalDocumentFile = fileDocumentPageableRepository.findFileDocumentByParentId(fileCommon.getId());

		var videoFile = optionalDocumentFile.orElseGet(FileDocument::new);
		// TODO DocumentTools which allows us to get the number of pages
		videoFile.setPages(0L);
		videoFile.setParentId(fileCommon.getId());

		fileDocumentPageableRepository.save(videoFile);
		resultMap.put(PublishResultEnum.OK, "Document pages updated, this is still unimplemented in the backend");

		return true;
	}
}
