package fi.poltsi.vempain.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import fi.poltsi.vempain.admin.api.response.AdminStatisticsResponse;
import fi.poltsi.vempain.admin.rest.StatisticsAPI;
import fi.poltsi.vempain.admin.service.StatisticsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class StatisticsController implements StatisticsAPI {

    private final StatisticsService statisticsService;

    @Override
    public ResponseEntity<AdminStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
}
