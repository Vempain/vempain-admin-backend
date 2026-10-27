package fi.poltsi.vempain.admin.service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.response.StatisticsCount;
import fi.poltsi.vempain.admin.api.response.StatisticsMonthlyCount;
import fi.poltsi.vempain.admin.api.response.StatisticsPublishState;
import fi.poltsi.vempain.admin.api.response.StatisticsUserCount;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The aggregate queries of the statistics service run against the real admin schema: counts, publish states, months, creators and paths of the generated pages,
 * site files, forms, layouts and components.
 */
class StatisticsServiceITC extends AbstractITCTest {

    @Autowired
    private StatisticsService statisticsService;

    @Test
    void countsTheGeneratedResourcesWithTheirBreakdowns() throws Exception {
        var pageId = testITCTools.generatePage();
        var page = pageRepository.findById(pageId)
            .orElseThrow();
        page.setPublished(Instant.now());
        pageRepository.save(page);
        var unpublishedPageId = testITCTools.generatePage();
        var siteFile = testITCTools.generateSiteFile(1);
        var month = DateTimeFormatter.ofPattern("yyyy-MM")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now());

        var statistics = statisticsService.getStatistics();

        assertThat(statistics.getGeneratedAt()).isNotNull();
        assertThat(count(statistics, "page")).isGreaterThanOrEqualTo(2);
        assertThat(count(statistics, "site_file")).isGreaterThanOrEqualTo(1);
        assertThat(count(statistics, "form")).isGreaterThanOrEqualTo(2);
        assertThat(count(statistics, "layout")).isGreaterThanOrEqualTo(1);
        assertThat(count(statistics, "component")).isGreaterThanOrEqualTo(3);

        assertThat(statistics.getFilesByType()).extracting(StatisticsCount::getName)
            .contains("IMAGE");
        var pageState = statistics.getPublishStates()
            .stream()
            .filter(state -> state.getResourceType()
                .equals("page"))
            .findFirst()
            .orElseThrow();
        assertThat(pageState.getPublished()).isGreaterThanOrEqualTo(1);
        assertThat(pageState.getNotPublished()).isGreaterThanOrEqualTo(1);
        assertThat(statistics.getPublishStates()).extracting(StatisticsPublishState::getResourceType)
            .containsExactly("page", "gallery", "site_file");

        assertThat(statistics.getCreatedByMonth()).anyMatch(entry -> entry.getResourceType()
            .equals("page")
                                                                     && entry.getMonth()
                                                                         .equals(month)
                                                                     && entry.getCount() >= 2);
        assertThat(statistics.getPublishedByMonth()).anyMatch(entry -> entry.getResourceType()
            .equals("page")
                                                                       && entry.getMonth()
                                                                           .equals(month)
                                                                       && entry.getCount() >= 1);
        assertThat(statistics.getCreatedByMonth()).extracting(StatisticsMonthlyCount::getMonth)
            .allMatch(value -> value.matches("\\d{4}-\\d{2}"));

        assertThat(statistics.getResourcesByUser()).anyMatch(row -> row.getResourceType()
            .equals("page")
                                                                    && row.getUserId() == page.getCreator()
                                                                    && row.getNick() != null
                                                                    && row.getCount() >= 1);
        assertThat(statistics.getResourcesByUser()).anyMatch(row -> row.getResourceType()
            .equals("site_file")
                                                                    && row.getUserId() == siteFile.getCreator());
        assertThat(statistics.getResourcesByUser()).extracting(StatisticsUserCount::getCount)
            .allMatch(value -> value > 0);

        assertThat(statistics.getPagesByForm()).isNotEmpty();
        assertThat(statistics.getFormsByLayout()).isNotEmpty();
        assertThat(statistics.getFormsByComponent()).isNotEmpty();
        assertThat(statistics.getPagesByPath()).extracting(StatisticsCount::getName)
            .contains("/test");
        assertThat(statistics.getFilesByPath()).extracting(StatisticsCount::getName)
            .contains(siteFile.getFilePath());
        assertThat(unpublishedPageId).isNotNull();
    }

    private static long count(fi.poltsi.vempain.admin.api.response.AdminStatisticsResponse statistics, String type) {
        return statistics.getResourceCounts()
            .stream()
            .filter(entry -> entry.getName()
                .equals(type))
            .mapToLong(StatisticsCount::getCount)
            .findFirst()
            .orElse(-1);
    }
}
