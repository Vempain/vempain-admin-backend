package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.api.QueryDetailEnum;
import fi.poltsi.vempain.admin.api.response.file.FileCommonResponse;
import fi.poltsi.vempain.admin.repository.file.FileCommonPageableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommonFileService {
	private final FileCommonPageableRepository fileCommonPageableRepository;
	public List<FileCommonResponse> findAllAsResponses(QueryDetailEnum requestForm) {
		var fileCommons = fileCommonPageableRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
		var responses = new ArrayList<FileCommonResponse>();

		for (var fileCommon : fileCommons) {
			var response = fileCommon.toResponse();
			responses.add(response);
		}

		return responses;
	}
}
