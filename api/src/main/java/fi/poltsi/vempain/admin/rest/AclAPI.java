package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.api.response.AclResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600) // TODO Remove before going to production
@Tag(name = "Acl", description = "Acl API for Vempain Acl objects")
public interface AclAPI {
	String MAIN_PATH = Constants.REST_CONTENT_PREFIX + "/acls";

	@Operation(summary = "Fetch list of all ACL items", description = "Returns a list of all Acl", tags = "Acl")
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got list of ACLs",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = AclResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No Acls found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<AclResponse>> getAllAcl();

	@Operation(summary = "Fetch list of ACL", description = "Returns a list of Acl with the user and unit data completed", tags = "Acl")
	@Parameter(name = "acl_id", example = "123", description = "ID of the list of ACLs to return", required = true)
	@ApiResponses(value = {@ApiResponse(responseCode = "200",
										description = "Got list of ACLs",
										content = {@Content(array = @ArraySchema(schema = @Schema(implementation = AclResponse.class)),
															mediaType = MediaType.APPLICATION_JSON_VALUE)}),
						   @ApiResponse(responseCode = "400", description = "Invalid request issued", content = @Content),
						   @ApiResponse(responseCode = "401", description = "Unauthorized access", content = @Content),
						   @ApiResponse(responseCode = "404", description = "No Acls found", content = @Content),
						   @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)})
	@SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = MAIN_PATH + "/{acl_id}", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<AclResponse>> getAcl(@PathVariable(name = "acl_id") Long aclId);
}
