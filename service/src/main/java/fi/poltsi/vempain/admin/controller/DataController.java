package fi.poltsi.vempain.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import fi.poltsi.vempain.admin.api.request.DataPagedRequest;
import fi.poltsi.vempain.admin.api.request.DataRequest;
import fi.poltsi.vempain.admin.api.response.DataResponse;
import fi.poltsi.vempain.admin.api.response.DataSetAclResponse;
import fi.poltsi.vempain.admin.api.response.DataSummaryResponse;
import fi.poltsi.vempain.admin.rest.DataAPI;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.admin.service.DataService;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import fi.poltsi.vempain.common.api.response.TaskAcceptedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Data sets are ACL-bearing resources: the listing shows the sets the caller may read and the other operations are checked against the set's ACL in
 * {@link DataService} (read, modify, delete). Creating a data set is an administrative action (the administrator ACL), because a new set gets an ACL of its own
 * owned by the caller.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class DataController implements DataAPI {

    private final DataService dataService;
    private final AccessService accessService;

    @Override
    public ResponseEntity<List<DataSummaryResponse>> getAllDataSets(String type, String identifierPrefix, String search) {
        accessService.checkAuthentication();
        log.debug("Received request to list all data sets with filters type='{}', identifierPrefix='{}', search='{}'", type, identifierPrefix, search);
        var summaries = dataService.findAll(type, identifierPrefix, search);
        return ResponseEntity.ok(summaries);
    }

    @Override
    public ResponseEntity<PagedResponse<DataSummaryResponse>> getPagedDataSets(DataPagedRequest request) {
        accessService.checkAuthentication();
        log.debug("Received paged data set request: page={}, size={}, sort_by={}, type={}, search='{}'", request.getPage(), request.getSize(),
                request.getSortBy(), request.getType(), request.getSearch());
        return ResponseEntity.ok(dataService.findPaged(request));
    }

    @Override
    public ResponseEntity<DataResponse> getDataSetByIdentifier(String identifier) {
        accessService.checkAuthentication();
        log.debug("Received request to get data set with identifier '{}'", identifier);
        var response = dataService.findByIdentifier(identifier);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DataSetAclResponse> getDataSetAcls(String identifier) {
        accessService.checkAuthentication();
        log.debug("Received request for the ACL grantees of data set '{}'", identifier);
        return ResponseEntity.ok(dataService.getAcls(identifier));
    }

    @Override
    public ResponseEntity<DataResponse> createDataSet(DataRequest dataRequest) {
        accessService.checkAdminAccess();
        log.debug("Received request to create a new data set: {}", dataRequest != null ? dataRequest.getIdentifier() : null);
        var response = dataService.create(dataRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DataResponse> updateDataSet(DataRequest dataRequest) {
        accessService.checkAuthentication();
        log.debug("Received request to update data set: {}", dataRequest != null ? dataRequest.getIdentifier() : null);
        var response = dataService.update(dataRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TaskAcceptedResponse> publishDataSet(String identifier) {
        accessService.checkAuthentication();
        log.debug("Received request to publish data set with identifier '{}'", identifier);
        var task = dataService.publishAsTask(identifier);
        return ResponseEntity.accepted()
            .body(task);
    }

    @Override
    public ResponseEntity<Void> deleteDataSet(String identifier) {
        accessService.checkAuthentication();
        log.debug("Received request to delete data set with identifier '{}'", identifier);
        dataService.delete(identifier);
        return ResponseEntity.noContent()
            .build();
    }
}
