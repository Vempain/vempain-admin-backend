package fi.poltsi.vempain.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import fi.poltsi.vempain.admin.api.request.TagBlacklistRequest;
import fi.poltsi.vempain.admin.api.response.TagBlacklistResponse;
import fi.poltsi.vempain.admin.rest.TagBlacklistAPI;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.admin.service.TagBlacklistService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TagBlacklistController implements TagBlacklistAPI {

    private final TagBlacklistService tagBlacklistService;
    private final AccessService accessService;

    @Override
    public ResponseEntity<List<TagBlacklistResponse>> getBlacklist() {
        accessService.checkAdminAccess();
        return ResponseEntity.ok(tagBlacklistService.findAll());
    }

    @Override
    public ResponseEntity<TagBlacklistResponse> addToBlacklist(TagBlacklistRequest request) {
        accessService.checkAdminAccess();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(tagBlacklistService.add(request, accessService.getValidUserId()));
    }

    @Override
    public ResponseEntity<Void> removeFromBlacklist(long entryId) {
        accessService.checkAdminAccess();
        tagBlacklistService.delete(entryId);
        return ResponseEntity.noContent()
            .build();
    }
}
