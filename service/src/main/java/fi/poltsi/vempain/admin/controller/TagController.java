package fi.poltsi.vempain.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import fi.poltsi.vempain.admin.api.response.TagResponse;
import fi.poltsi.vempain.admin.rest.TagAPI;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.admin.service.SubjectService;
import lombok.RequiredArgsConstructor;

/**
 * Tag suggestions for the page editor. Tags are metadata without an ACL, so any signed-in user may list them; nothing is created here.
 */
@RestController
@RequiredArgsConstructor
public class TagController implements TagAPI {

    private final SubjectService subjectService;
    private final AccessService accessService;

    @Override
    public ResponseEntity<List<TagResponse>> findTags(String search, Integer limit) {
        accessService.getValidUserId();
        return ResponseEntity.ok(subjectService.searchTags(search, limit));
    }
}
