package fi.poltsi.vempain.admin.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fi.poltsi.vempain.admin.api.response.AdminStatisticsResponse;
import fi.poltsi.vempain.admin.service.StatisticsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsControllerUTC {

    @Mock
    private StatisticsService statisticsService;
    @InjectMocks
    private StatisticsController controller;

    @Test
    void answersTheStatisticsOfTheService() {
        var statistics = AdminStatisticsResponse.builder()
            .build();
        when(statisticsService.getStatistics()).thenReturn(statistics);

        var response = controller.getStatistics();

        assertEquals(200, response.getStatusCode()
            .value());
        assertSame(statistics, response.getBody());
    }
}
