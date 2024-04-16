package fi.poltsi.vempain.admin.controller.file;

import fi.poltsi.vempain.admin.api.QueryDetailEnum;
import fi.poltsi.vempain.admin.api.response.file.FileCommonResponse;
import fi.poltsi.vempain.admin.rest.file.CommonFileAPI;
import fi.poltsi.vempain.admin.service.file.CommonFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
public class CommonFileController implements CommonFileAPI {
	private final CommonFileService commonFileService;

	@Override
	public ResponseEntity<List<FileCommonResponse>> getCommonFiles(QueryDetailEnum requestForm) {
		return ResponseEntity.ok(commonFileService.findAllAsResponses(requestForm));
	}
}
