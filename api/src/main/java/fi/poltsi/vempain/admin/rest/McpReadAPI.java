package fi.poltsi.vempain.admin.rest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import fi.poltsi.vempain.admin.api.Constants;
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
import fi.poltsi.vempain.auth.api.response.AclResponse;
import fi.poltsi.vempain.auth.api.response.UnitResponse;
import fi.poltsi.vempain.auth.api.response.UserResponse;

public interface McpReadAPI {

    String MAIN_PATH = Constants.REST_MCP_PREFIX;

    @GetMapping(value = MAIN_PATH + "/pages", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<McpPagedResponse<PageResponse>> getPages(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size);

    @GetMapping(value = MAIN_PATH + "/pages/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PageResponse> getPage(@PathVariable("id") long id);

    @GetMapping(value = MAIN_PATH + "/galleries", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<McpPagedResponse<GalleryResponse>> getGalleries(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size);

    @GetMapping(value = MAIN_PATH + "/galleries/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<GalleryResponse> getGallery(@PathVariable("id") long id);

    @GetMapping(value = MAIN_PATH + "/site-files", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<McpPagedResponse<SiteFileResponse>> getSiteFiles(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size);

    @GetMapping(value = MAIN_PATH + "/data-sets", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<McpPagedResponse<DataSummaryResponse>> getDataSets(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size);

    @GetMapping(value = MAIN_PATH + "/layouts", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<McpPagedResponse<LayoutResponse>> getLayouts(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size);

    @GetMapping(value = MAIN_PATH + "/components", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<McpPagedResponse<ComponentResponse>> getComponents(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size);

    @GetMapping(value = MAIN_PATH + "/forms", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<McpPagedResponse<FormResponse>> getForms(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size);

    @GetMapping(value = MAIN_PATH + "/schedules", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<McpPagedResponse<PublishScheduleResponse>> getSchedules(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size);

    @GetMapping(value = MAIN_PATH + "/tags", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<McpPagedResponse<TagResponse>> getTags(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size);

    @GetMapping(value = MAIN_PATH + "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<McpPagedResponse<UserResponse>> getUsers(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size);

    @GetMapping(value = MAIN_PATH + "/units", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<McpPagedResponse<UnitResponse>> getUnits(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size);

    @GetMapping(value = MAIN_PATH + "/acls", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<McpPagedResponse<AclResponse>> getAcls(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size);

    @GetMapping(value = MAIN_PATH + "/web-site/pages", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<McpPagedResponse<McpWebSitePageResponse>> getPublishedPages(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size);

    @GetMapping(value = MAIN_PATH + "/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AdminStatisticsResponse> getStatistics();
}
