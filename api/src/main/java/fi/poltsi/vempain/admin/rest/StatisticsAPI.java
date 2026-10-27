package fi.poltsi.vempain.admin.rest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.response.AdminStatisticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * System wide resource statistics of the admin backend, shown on the Statistics screen of the admin frontend. The counts are not filtered by ACL: they describe
 * the whole database, like the inventory metrics.
 */
@Tag(name = "StatisticsAPI", description = "Counts and breakdowns of the resources managed by the admin backend")
@SecurityRequirement(name = "Bearer Authentication")
public interface StatisticsAPI {

    String BASE_PATH = Constants.REST_ADMIN_PREFIX + "/statistics";

    @Operation(summary = "Get resource statistics",
            description = "Counts of pages, galleries, site files, components, forms and layouts with breakdowns by file type, publish state, month, "
                    + "user, form, layout, component and path",
            tags = "StatisticsAPI")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics",
                content = @Content(schema = @Schema(implementation = AdminStatisticsResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
    @GetMapping(path = BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AdminStatisticsResponse> getStatistics();
}
