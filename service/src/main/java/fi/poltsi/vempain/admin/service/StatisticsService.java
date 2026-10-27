package fi.poltsi.vempain.admin.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fi.poltsi.vempain.admin.api.response.AdminStatisticsResponse;
import fi.poltsi.vempain.admin.api.response.StatisticsCount;
import fi.poltsi.vempain.admin.api.response.StatisticsMonthlyCount;
import fi.poltsi.vempain.admin.api.response.StatisticsPublishState;
import fi.poltsi.vempain.admin.api.response.StatisticsUserCount;
import lombok.RequiredArgsConstructor;

/**
 * Counts the resources of the admin database for the Statistics screen. The counts are taken with native aggregate queries so that the database does the
 * grouping; every query is a fixed string over the known resource tables, nothing comes from the request. Like the inventory metrics the numbers are system
 * wide and deliberately not filtered by ACL.
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    /**
     * The resource tables in the order the response lists them. Every table extends {@code AbstractVempainEntity} (creator, created); the publishable ones also
     * carry a {@code published} timestamp.
     */
    static final List<Resource> RESOURCES = List.of(new Resource("page", "page", true), new Resource("gallery", "gallery", true),
            new Resource("site_file", "site_file", true), new Resource("component", "component", false), new Resource("form", "form", false),
            new Resource("layout", "layout", false));

    record Resource(String type, String table, boolean publishable) {}

    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public AdminStatisticsResponse getStatistics() {
        var resourceCounts = new ArrayList<StatisticsCount>();
        var publishStates = new ArrayList<StatisticsPublishState>();
        var publishedByMonth = new ArrayList<StatisticsMonthlyCount>();
        var createdByMonth = new ArrayList<StatisticsMonthlyCount>();
        var resourcesByUser = new ArrayList<StatisticsUserCount>();

        for (var resource : RESOURCES) {
            resourceCounts.add(new StatisticsCount(resource.type(), count("SELECT COUNT(*) FROM " + resource.table())));
            createdByMonth.addAll(monthly(resource, "created"));
            if (resource.publishable()) {
                var row = rows("SELECT COUNT(*) FILTER (WHERE published IS NOT NULL), COUNT(*) FILTER (WHERE published IS NULL) FROM " + resource.table())
                    .getFirst();
                publishStates.add(new StatisticsPublishState(resource.type(), asLong(row[0]), asLong(row[1])));
                publishedByMonth.addAll(monthly(resource, "published"));
            }
            for (var userRow : rows("SELECT t.creator, u.nick, COUNT(*) FROM "
                    + resource.table()
                    + " t LEFT JOIN user_account u ON u.id = t.creator GROUP BY t.creator, u.nick ORDER BY 3 DESC, 1")) {
                resourcesByUser.add(new StatisticsUserCount(asLong(userRow[0]), (String) userRow[1], resource.type(), asLong(userRow[2])));
            }
        }

        return AdminStatisticsResponse.builder()
            .generatedAt(Instant.now())
            .resourceCounts(resourceCounts)
            .filesByType(counts("SELECT file_type, COUNT(*) FROM site_file GROUP BY file_type ORDER BY 2 DESC, 1"))
            .publishStates(publishStates)
            .publishedByMonth(publishedByMonth)
            .createdByMonth(createdByMonth)
            .resourcesByUser(resourcesByUser)
            .pagesByForm(counts("SELECT f.form_name, COUNT(*) FROM page p JOIN form f ON f.id = p.form_id GROUP BY f.form_name ORDER BY 2 DESC, 1"))
            .formsByLayout(counts("SELECT l.layout_name, COUNT(*) FROM form f JOIN layout l ON l.id = f.layout_id GROUP BY l.layout_name ORDER BY 2 DESC, 1"))
            .formsByComponent(counts("SELECT c.comp_name, COUNT(DISTINCT fc.form_id) FROM form_component fc JOIN component c ON c.id = fc.component_id "
                    + "GROUP BY c.comp_name ORDER BY 2 DESC, 1"))
            .pagesByPath(counts("SELECT COALESCE(NULLIF(regexp_replace(page_path, '/[^/]*$', ''), ''), '/'), COUNT(*) FROM page GROUP BY 1 ORDER BY 2 DESC, 1"))
            .filesByPath(counts("SELECT file_path, COUNT(*) FROM site_file GROUP BY file_path ORDER BY 2 DESC, 1"))
            .build();
    }

    /**
     * Resources of a type per calendar month (UTC) of a timestamp column, months without resources left out.
     */
    private List<StatisticsMonthlyCount> monthly(Resource resource, String column) {
        var sql = "SELECT to_char(date_trunc('month', "
                + column
                + " AT TIME ZONE 'UTC'), 'YYYY-MM'), COUNT(*) FROM "
                + resource.table()
                + " WHERE "
                + column
                + " IS NOT NULL GROUP BY 1 ORDER BY 1";
        return rows(sql).stream()
            .map(row -> new StatisticsMonthlyCount(resource.type(), (String) row[0], asLong(row[1])))
            .toList();
    }

    private List<StatisticsCount> counts(String sql) {
        return rows(sql).stream()
            .map(row -> new StatisticsCount(String.valueOf(row[0]), asLong(row[1])))
            .toList();
    }

    private long count(String sql) {
        return asLong(entityManager.createNativeQuery(sql)
            .getSingleResult());
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql) {
        return entityManager.createNativeQuery(sql)
            .getResultList();
    }

    private static long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
