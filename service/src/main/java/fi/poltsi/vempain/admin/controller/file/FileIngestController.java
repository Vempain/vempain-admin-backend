package fi.poltsi.vempain.admin.controller.file;

import fi.poltsi.vempain.admin.api.request.file.FileIngestRequest;
import fi.poltsi.vempain.admin.api.response.file.FileIngestResponse;
import fi.poltsi.vempain.admin.rest.file.FileIngestApi;
import fi.poltsi.vempain.admin.service.file.FileIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FileIngestController implements FileIngestApi {
	private final FileIngestService fileIngestService;

	@Override
	public ResponseEntity<FileIngestResponse> ingest(FileIngestRequest fileIngestRequest, MultipartFile multipartFile) {
		try {
			var result = fileIngestService.ingest(fileIngestRequest, multipartFile);
			return ResponseEntity.ok(result);
		} catch (IllegalArgumentException e) {
			log.warn("Bad request in file ingest: {}", e.getMessage());
			return ResponseEntity.badRequest().build();
		} catch (AccessDeniedException e) {
			log.warn("Unauthorized S2S call: {}", e.getMessage());
			return ResponseEntity.status(401).build();
		} catch (Exception e) {
			log.error("File ingest failed", e);
			return ResponseEntity.internalServerError().build();
		}
	}
}
