package fi.poltsi.vempain.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import fi.poltsi.vempain.admin.api.response.AdminStatisticsResponse;
import fi.poltsi.vempain.admin.api.response.ComponentResponse;
import fi.poltsi.vempain.admin.api.response.DataSummaryResponse;
import fi.poltsi.vempain.admin.api.response.FormResponse;
import fi.poltsi.vempain.admin.api.response.LayoutResponse;
import fi.poltsi.vempain.admin.api.response.McpPagedResponse;
import fi.poltsi.vempain.admin.api.response.PageResponse;
import fi.poltsi.vempain.admin.api.response.PublishScheduleResponse;
import fi.poltsi.vempain.admin.api.response.TagResponse;
import fi.poltsi.vempain.admin.api.response.file.GalleryResponse;
import fi.poltsi.vempain.admin.api.response.file.SiteFileResponse;
import fi.poltsi.vempain.admin.api.site.response.McpWebSitePageResponse;
import fi.poltsi.vempain.admin.rest.McpReadAPI;
import fi.poltsi.vempain.admin.security.ApiTokenAuthenticationFilter;
import fi.poltsi.vempain.admin.service.McpReadService;
import fi.poltsi.vempain.auth.api.response.AclResponse;
import fi.poltsi.vempain.auth.api.response.UnitResponse;
import fi.poltsi.vempain.auth.api.response.UserResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + ApiTokenAuthenticationFilter.API_TOKEN_MCP_READ_AUTHORITY + "')")
public class McpReadController implements McpReadAPI {

    private final McpReadService mcpReadService;

    @Override
    public ResponseEntity<McpPagedResponse<PageResponse>> getPages(int page, int size) {
        return ResponseEntity.ok(mcpReadService.pages(page, size));
    }

    @Override
    public ResponseEntity<PageResponse> getPage(long id) {
        return ResponseEntity.ok(mcpReadService.page(id));
    }

    @Override
    public ResponseEntity<McpPagedResponse<GalleryResponse>> getGalleries(int page, int size) {
        return ResponseEntity.ok(mcpReadService.galleries(page, size));
    }

    @Override
    public ResponseEntity<GalleryResponse> getGallery(long id) {
        return ResponseEntity.ok(mcpReadService.gallery(id));
    }

    @Override
    public ResponseEntity<McpPagedResponse<SiteFileResponse>> getSiteFiles(int page, int size) {
        return ResponseEntity.ok(mcpReadService.siteFiles(page, size));
    }

    @Override
    public ResponseEntity<McpPagedResponse<DataSummaryResponse>> getDataSets(int page, int size) {
        return ResponseEntity.ok(mcpReadService.dataSets(page, size));
    }

    @Override
    public ResponseEntity<McpPagedResponse<LayoutResponse>> getLayouts(int page, int size) {
        return ResponseEntity.ok(mcpReadService.layouts(page, size));
    }

    @Override
    public ResponseEntity<McpPagedResponse<ComponentResponse>> getComponents(int page, int size) {
        return ResponseEntity.ok(mcpReadService.components(page, size));
    }

    @Override
    public ResponseEntity<McpPagedResponse<FormResponse>> getForms(int page, int size) {
        return ResponseEntity.ok(mcpReadService.forms(page, size));
    }

    @Override
    public ResponseEntity<McpPagedResponse<PublishScheduleResponse>> getSchedules(int page, int size) {
        return ResponseEntity.ok(mcpReadService.schedules(page, size));
    }

    @Override
    public ResponseEntity<McpPagedResponse<TagResponse>> getTags(int page, int size) {
        return ResponseEntity.ok(mcpReadService.tags(page, size));
    }

    @Override
    public ResponseEntity<McpPagedResponse<UserResponse>> getUsers(int page, int size) {
        return ResponseEntity.ok(mcpReadService.users(page, size));
    }

    @Override
    public ResponseEntity<McpPagedResponse<UnitResponse>> getUnits(int page, int size) {
        return ResponseEntity.ok(mcpReadService.units(page, size));
    }

    @Override
    public ResponseEntity<McpPagedResponse<AclResponse>> getAcls(int page, int size) {
        return ResponseEntity.ok(mcpReadService.acls(page, size));
    }

    @Override
    public ResponseEntity<McpPagedResponse<McpWebSitePageResponse>> getPublishedPages(int page, int size) {
        return ResponseEntity.ok(mcpReadService.publishedPages(page, size));
    }

    @Override
    public ResponseEntity<AdminStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(mcpReadService.statistics());
    }
}
