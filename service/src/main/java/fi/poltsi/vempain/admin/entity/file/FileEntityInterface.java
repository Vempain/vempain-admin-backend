package fi.poltsi.vempain.admin.entity.file;

import fi.poltsi.vempain.admin.api.response.file.AbstractFileResponse;

public interface FileEntityInterface<R extends AbstractFileResponse> {
	R toResponse();
}
