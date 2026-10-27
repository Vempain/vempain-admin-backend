package fi.poltsi.vempain.admin.service;

import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fi.poltsi.vempain.admin.api.response.StatisticsCount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The statistics service turns the rows of its aggregate queries into the response: one query per table and breakdown, numbers of any JDBC type.
 */
@ExtendWith(MockitoExtension.class)
class StatisticsServiceUTC {

    @Mock
    private EntityManager entityManager;

    private StatisticsService service;

    /** Canned result rows keyed by a distinctive part of the SQL; unknown queries answer no rows. */
    private final Map<String, List<Object[]>> rows = Map.ofEntries(
            Map.entry("COUNT(*) FILTER (WHERE published IS NOT NULL), COUNT(*) FILTER (WHERE published IS NULL) FROM page", rowsOf(new Object[]{ 3L, 1L })),
            Map.entry("COUNT(*) FILTER (WHERE published IS NOT NULL), COUNT(*) FILTER (WHERE published IS NULL) FROM gallery", rowsOf(new Object[]{ 0L, 2L })),
            Map.entry("COUNT(*) FILTER (WHERE published IS NOT NULL), COUNT(*) FILTER (WHERE published IS NULL) FROM site_file",
                    rowsOf(new Object[]{ 10L, 0L })),
            Map.entry("date_trunc('month', created AT TIME ZONE 'UTC'), 'YYYY-MM'), COUNT(*) FROM page",
                    rowsOf(new Object[]{ "2026-09", 1L }, new Object[]{ "2026-10", 3L })),
            Map.entry("date_trunc('month', published AT TIME ZONE 'UTC'), 'YYYY-MM'), COUNT(*) FROM page", rowsOf(new Object[]{ "2026-10", 3L })),
            Map.entry("FROM page t LEFT JOIN user_account u", rowsOf(new Object[]{ 1L, "admin", 3L }, new Object[]{ 9L, null, 1L })),
            Map.entry("SELECT file_type, COUNT(*) FROM site_file", rowsOf(new Object[]{ "IMAGE", 8 }, new Object[]{ "VIDEO", 2 })),
            Map.entry("JOIN form f ON f.id = p.form_id", rowsOf(new Object[]{ "Article", 4L })),
            Map.entry("JOIN layout l ON l.id = f.layout_id", rowsOf(new Object[]{ "Main", 2L })),
            Map.entry("FROM form_component fc", rowsOf(new Object[]{ "Header", 2L })),
            Map.entry("regexp_replace(page_path", rowsOf(new Object[]{ "/blog", 3L }, new Object[]{ "/", 1L })),
            Map.entry("SELECT file_path, COUNT(*) FROM site_file", rowsOf(new Object[]{ "/uploads/2026/", 10L })));

    private static List<Object[]> rowsOf(Object[]... rows) {
        return List.of(rows);
    }

    @BeforeEach
    void setUp() {
        when(entityManager.createNativeQuery(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            var query = mock(Query.class);
            if (sql.startsWith("SELECT COUNT(*) FROM ")) {
                when(query.getSingleResult()).thenReturn(sql.endsWith("site_file") ? Integer.valueOf(10) : Long.valueOf(4));
            } else {
                when(query.getResultList()).thenReturn(rows.entrySet()
                    .stream()
                    .filter(entry -> sql.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(List.of()));
            }
            return query;
        });
        service = new StatisticsService(entityManager);
    }

    @Test
    void collectsTheCountsOfEveryResourceAndItsBreakdowns() {
        var statistics = service.getStatistics();

        assertThat(statistics.getGeneratedAt()).isNotNull();
        assertThat(statistics.getResourceCounts()).extracting(StatisticsCount::getName)
            .containsExactly("page", "gallery", "site_file", "component", "form", "layout");
        assertThat(statistics.getResourceCounts()).extracting(StatisticsCount::getCount)
            .containsExactly(4L, 4L, 10L, 4L, 4L, 4L);
        assertThat(statistics.getFilesByType()).extracting(StatisticsCount::getName, StatisticsCount::getCount)
            .containsExactly(org.assertj.core.groups.Tuple.tuple("IMAGE", 8L), org.assertj.core.groups.Tuple.tuple("VIDEO", 2L));

        assertThat(statistics.getPublishStates()).hasSize(3);
        var pages = statistics.getPublishStates()
            .getFirst();
        assertThat(pages.getResourceType()).isEqualTo("page");
        assertThat(pages.getPublished()).isEqualTo(3);
        assertThat(pages.getNotPublished()).isEqualTo(1);

        assertThat(statistics.getCreatedByMonth()).extracting(m -> m.getResourceType() + " " + m.getMonth() + "=" + m.getCount())
            .containsExactly("page 2026-09=1", "page 2026-10=3");
        assertThat(statistics.getPublishedByMonth()).extracting(m -> m.getResourceType() + " " + m.getMonth() + "=" + m.getCount())
            .containsExactly("page 2026-10=3");

        assertThat(statistics.getResourcesByUser()).hasSize(2);
        assertThat(statistics.getResourcesByUser()
            .getFirst()
            .getNick()).isEqualTo("admin");
        assertThat(statistics.getResourcesByUser()
            .get(1)
            .getNick()).isNull();
        assertThat(statistics.getResourcesByUser()
            .get(1)
            .getUserId()).isEqualTo(9);

        assertThat(statistics.getPagesByForm()).extracting(StatisticsCount::getName)
            .containsExactly("Article");
        assertThat(statistics.getFormsByLayout()).extracting(StatisticsCount::getName)
            .containsExactly("Main");
        assertThat(statistics.getFormsByComponent()).extracting(StatisticsCount::getName)
            .containsExactly("Header");
        assertThat(statistics.getPagesByPath()).extracting(StatisticsCount::getName)
            .containsExactly("/blog", "/");
        assertThat(statistics.getFilesByPath()).extracting(StatisticsCount::getCount)
            .containsExactly(10L);
    }
}
