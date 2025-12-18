package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.rest.AclAPI;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.auth.api.response.AclResponse;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.service.AclService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@Slf4j
@RequiredArgsConstructor
@RestController
public class AclController implements AclAPI {
	private final AclService    aclService;
	private final AccessService accessService;

	@Override
	public ResponseEntity<List<AclResponse>> getAllAcl() {
		accessService.checkAuthentication();

		Iterable<Acl> aclData = aclService.findAll();

		return getListResponseEntity(aclData);
	}

	@Override
	public ResponseEntity<List<AclResponse>> getAcl(Long aclId) {
		accessService.checkAuthentication();
		log.debug("Acl REST API called with aclId: {}", aclId);
		log.debug("Retrieving all acl info based on ID");
		Iterable<Acl> aclData = aclService.findAclByAclId(aclId);

		if (StreamSupport.stream(aclData.spliterator(), false)
						 .findAny()
						 .isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "There are no ACL in the database");
		}

		return getListResponseEntity(aclData);
	}

	private ResponseEntity<List<AclResponse>> getListResponseEntity(Iterable<Acl> aclData) {
		ArrayList<AclResponse> responses = new ArrayList<>();

		for (Acl acl : aclData) {
			responses.add(acl.toResponse());
		}

		return ResponseEntity.ok(responses);
	}
}
