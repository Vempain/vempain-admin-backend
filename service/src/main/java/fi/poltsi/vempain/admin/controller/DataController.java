package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.api.request.DataRequest;
import fi.poltsi.vempain.admin.api.response.DataResponse;
import fi.poltsi.vempain.admin.api.response.DataSummaryResponse;
import fi.poltsi.vempain.admin.rest.DataAPI;
import fi.poltsi.vempain.admin.service.DataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class DataController implements DataAPI {
	private final DataService dataService;

	@Override
	public ResponseEntity<List<DataSummaryResponse>> getAllDataSets() {
		log.debug("Received request to list all data sets");
		var summaries = dataService.findAll();
		return ResponseEntity.ok(summaries);
	}

	@Override
	public ResponseEntity<DataResponse> getDataSetByIdentifier(String identifier) {
		log.debug("Received request to get data set with identifier '{}'", identifier);
		var response = dataService.findByIdentifier(identifier);
		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<DataResponse> createDataSet(DataRequest dataRequest) {
		log.debug("Received request to create a new data set: {}", dataRequest != null ? dataRequest.getIdentifier() : null);
		var response = dataService.create(dataRequest);
		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<DataResponse> updateDataSet(DataRequest dataRequest) {
		log.debug("Received request to update data set: {}", dataRequest != null ? dataRequest.getIdentifier() : null);
		var response = dataService.update(dataRequest);
		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<DataResponse> publishDataSet(String identifier) {
		log.debug("Received request to publish data set with identifier '{}'", identifier);
		var response = dataService.publish(identifier);
		return ResponseEntity.ok(response);
	}
}
