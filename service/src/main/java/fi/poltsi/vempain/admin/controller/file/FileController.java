package fi.poltsi.vempain.admin.controller.file;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.request.FileProcessRequest;
import fi.poltsi.vempain.admin.api.response.StringList;
import fi.poltsi.vempain.admin.api.response.file.FileAudioResponse;
import fi.poltsi.vempain.admin.api.response.file.FileCommonResponse;
import fi.poltsi.vempain.admin.api.response.file.FileDocumentResponse;
import fi.poltsi.vempain.admin.api.response.file.FileImageResponse;
import fi.poltsi.vempain.admin.api.response.file.FileVideoResponse;
import fi.poltsi.vempain.admin.entity.file.FileCommon;
import fi.poltsi.vempain.admin.exception.VempainAclException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.rest.file.FileAPI;
import fi.poltsi.vempain.admin.service.file.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriFunction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class FileController implements FileAPI {
	private final FileService fileService;

	@Override
	public ResponseEntity<Page<FileAudioResponse>> getPageableAudioFiles(int pageNumber, int pageSize, String sorting, String filter, String filterColumn) {
		return getPageableFiles(pageNumber, pageSize, sorting, filter, filterColumn,
								FileAudioResponse.class,
								fileService::findAllAudiosAsPageableResponseFiltered);
	}

	@Override
	public ResponseEntity<Page<FileDocumentResponse>> getPageableDocumentFiles(int pageNumber, int pageSize, String sorting, String filter, String filterColumn) {
		return getPageableFiles(pageNumber, pageSize, sorting, filter, filterColumn,
								FileDocumentResponse.class,
								fileService::findAllDocumentsAsPageableResponseFiltered);
	}

	@Override
	public ResponseEntity<Page<FileImageResponse>> getPageableImageFiles(int pageNumber, int pageSize, String sorting, String filter, String filterColumn) {
		return getPageableFiles(pageNumber, pageSize, sorting, filter, filterColumn,
								FileImageResponse.class,
								fileService::findAllImagesAsPageableResponseFiltered);
	}

	@Override
	public ResponseEntity<FileImageResponse> getImageById(long imageId) {
		var imageResponse = fileService.findImageById(imageId);

		return ResponseEntity.ok(imageResponse);
	}

	@Override
	public ResponseEntity<Page<FileVideoResponse>> getPageableVideoFiles(int pageNumber, int pageSize, String sorting, String filter, String filterColumn) {
		return getPageableFiles(pageNumber, pageSize, sorting, filter, filterColumn,
								FileVideoResponse.class,
								fileService::findAllVideosAsPageableResponseFiltered);
	}

	@Override
	public ResponseEntity<List<FileCommonResponse>> addFilesFromDirectory(FileProcessRequest fileProcessRequest) {
		log.info("Received request: {}", fileProcessRequest);
		List<FileCommon> fileCommonList;
		try {
			fileCommonList = fileService.addFilesFromDirectory(fileProcessRequest);
			var fileCommonResponseList = new ArrayList<FileCommonResponse>();

			for (var fileCommon : fileCommonList) {
				fileCommonResponseList.add(fileCommon.toResponse());
			}

			return ResponseEntity.ok(fileCommonResponseList);
		} catch (VempainEntityNotFoundException | IOException | VempainAclException e) {
			log.error("Failed to import files with request {}", fileProcessRequest, e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
		}
	}

	@Override
	public ResponseEntity<List<FileCommonResponse>> upload(MultipartFile[] multipartFiles,
														   String destinationDirectory,
														   String galleryShortname,
														   String galleryDescription) {
		var fileCommonResponseList = new ArrayList<FileCommonResponse>();
		fileService.processUploadFiles(multipartFiles, destinationDirectory, galleryShortname, galleryDescription);
		return ResponseEntity.ok(fileCommonResponseList);
	}

	@Override
	public ResponseEntity<List<String>> importDirectories() {
		var importDirectories = fileService.getConvertedDirectories();
		Collections.sort(importDirectories);
		return ResponseEntity.ok(importDirectories);
	}

	@Override
	public ResponseEntity<StringList> importDirectoryMatch(String path) {
		var matchedDirectories = fileService.matchConvertedDirectories(path);

		var parentDirectories = new ArrayList<String>();

		for (var matchedDirectory : matchedDirectories) {
			// Split the matched directory to each part
			var splitted = matchedDirectory.split(File.separator);
			var compoundPath = splitted[0];
			parentDirectories.add(compoundPath);

			for (int i = 1; i < splitted.length; i++) {
				compoundPath += File.separator + splitted[i];
				parentDirectories.add(compoundPath);
			}
		}

		// Then add the unique parent directories to the list
		matchedDirectories.addAll(parentDirectories);
		// Add also the root to the list
		matchedDirectories.add(File.separator);
		// Remove duplicates by temporarily casting to a Set and back to a List
		var tmpSet = new HashSet<>(matchedDirectories);
		matchedDirectories = new ArrayList<>(tmpSet);
		// Remove empty string from the list
		matchedDirectories.remove("");
		// Sort the list
		Collections.sort(matchedDirectories);
		return ResponseEntity.ok(StringList.builder()
										   .stringList(matchedDirectories)
										   .build());
	}

	private <T> ResponseEntity<Page<T>> getPageableFiles(int pageNumber, int pageSize, String sorting, String filter, String filterColumn,
														 Class<T> responseType,
														 TriFunction<PageRequest, String, String, Page<T>> findAllFilesAsPageableResponseFilteredFunction) {
		log.debug("Received request for pageable {} files with pageNumber: {}, pageSize: {}, sorting: {}, filter: {}, filterColumn: {}",
				  responseType.getName(), pageNumber, pageSize, sorting, filter, filterColumn);

		var     pageRequest = getPageRequest(pageNumber, pageSize, sorting);
		Page<T> pageableFileResponse;

		pageableFileResponse = findAllFilesAsPageableResponseFilteredFunction.apply(pageRequest, filter, filterColumn);

		return ResponseEntity.ok(pageableFileResponse);
	}

	private PageRequest getPageRequest(int pageNumber, int pageSize, String sorting) {
		if (pageNumber < 0) {
			pageNumber = 0;
		}
		if (pageSize < 0) {
			pageSize = 10;
		}

		// Parse sorting string to separate strings for field and direction
		var column    = sorting.split(",")[0];
		var direction = sorting.split(",")[1];

		// The frontend sends the direction as "ascend" or "descend", but Spring expects "ASC" or "DESC", so we convert
		if (direction.equals("ascend")) {
			direction = "ASC";
		} else {
			direction = "DESC";
		}

		var sort = Sort.by(Sort.Direction.fromString(direction), column);
		return PageRequest.of(pageNumber, pageSize, sort);
	}
}
