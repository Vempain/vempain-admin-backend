package fi.poltsi.vempain.admin.controller.file;

import fi.poltsi.vempain.admin.api.response.file.DirectoryNodeResponse;
import fi.poltsi.vempain.admin.rest.file.FileSystemAPI;
import fi.poltsi.vempain.admin.service.file.FileSystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class FileSystemController implements FileSystemAPI {
	private final FileSystemService fileSystemService;

	@Override
	public ResponseEntity<List<DirectoryNodeResponse>> getConvertedDirectoryStructure() {
		return ResponseEntity.ok(fileSystemService.getConvertedDirectoryTree());
	}
}
