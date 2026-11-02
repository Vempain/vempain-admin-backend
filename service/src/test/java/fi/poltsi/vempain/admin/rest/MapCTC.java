package fi.poltsi.vempain.admin.rest;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import fi.poltsi.vempain.admin.entity.file.FileThumb;
import fi.poltsi.vempain.admin.entity.file.GpsLocation;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.repository.GpsLocationRepository;
import fi.poltsi.vempain.admin.repository.file.FileThumbPageableRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.common.api.FileTypeEnum;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The clustered map of the GPS-tagged site files against the real schema (three readable site files around Helsinki, one the administrator may not read, one in
 * Tampere; a subject on one of them and a thumbnail of another), and the thumbnail endpoint the map's previews use.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MapCTC {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private AclService aclService;
    @Autowired
    private SiteFileRepository siteFileRepository;
    @Autowired
    private GpsLocationRepository gpsLocationRepository;
    @Autowired
    private FileThumbPageableRepository fileThumbPageableRepository;
    @Autowired
    @Qualifier("adminDataSource")
    private DataSource adminDataSource;
    @Value("${vempain.admin.file.site-file-directory}")
    private String siteFileDirectory;

    private JdbcTemplate jdbcTemplate;
    private long nextLocationId = 9_300_001L;
    private long helsinkiA;
    private long helsinkiB;
    private long helsinkiVideo;
    private long denied;
    private long tampere;
    private long subjectId;
    private long thumbId;
    private Path thumbFile;

    @BeforeEach
    void seed() throws Exception {
        jdbcTemplate = new JdbcTemplate(adminDataSource);
        cleanStale();
        helsinkiA = seedSiteFile("map_helsinki_a.jpg", FileTypeEnum.IMAGE, "image/jpeg", 60.17123, 24.94156, "2026-06-01T12:00:00Z", true);
        helsinkiB = seedSiteFile("map_helsinki_b.jpg", FileTypeEnum.IMAGE, "image/jpeg", 60.17456, 24.94890, "2026-07-01T12:00:00Z", true);
        helsinkiVideo = seedSiteFile("map_helsinki.mp4", FileTypeEnum.VIDEO, "video/mp4", 60.16890, 24.93210, "2026-08-01T12:00:00Z", true);
        denied = seedSiteFile("map_secret.jpg", FileTypeEnum.IMAGE, "image/jpeg", 60.17000, 24.94000, "2026-06-15T12:00:00Z", false);
        tampere = seedSiteFile("map_tampere.jpg", FileTypeEnum.IMAGE, "image/jpeg", 61.49800, 23.76100, "2025-12-24T12:00:00Z", true);
        // a subject on the second image
        jdbcTemplate.update("INSERT INTO subjects(subject) VALUES ('ctc-map-summer')");
        subjectId = jdbcTemplate.queryForObject("SELECT id FROM subjects WHERE subject = 'ctc-map-summer'", Long.class);
        jdbcTemplate.update("INSERT INTO file_subject(site_file_id, subject_id) VALUES (?, ?)", helsinkiB, subjectId);
        // a thumbnail of the first image, as FileThumbService stores them: <site-file-directory>/<filepath>/<filename>
        var thumbDir = Path.of(siteFileDirectory, "thumb", "image", "ctc-map");
        Files.createDirectories(thumbDir);
        thumbFile = thumbDir.resolve("map_helsinki_a.jpeg");
        Files.write(thumbFile, new byte[]{ (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9 });
        thumbId = fileThumbPageableRepository.save(FileThumb.builder()
            .parentId(helsinkiA)
            .parentType(FileTypeEnum.IMAGE)
            .filepath("thumb/image/ctc-map")
            .filename("map_helsinki_a.jpeg")
            .filesize(4L)
            .width(10)
            .height(10)
            .sha1sum("x")
            .build())
            .getId();
    }

    @AfterEach
    void clean() throws Exception {
        Files.deleteIfExists(thumbFile);
        cleanStale();
    }

    /** Removes everything this class seeds, also what an earlier failed run left behind */
    private void cleanStale() {
        jdbcTemplate.update("DELETE FROM file_thumb WHERE parent_id IN (SELECT id FROM site_file WHERE file_path = 'ctc-map')");
        jdbcTemplate.update("DELETE FROM file_subject WHERE subject_id IN (SELECT id FROM subjects WHERE subject = 'ctc-map-summer')");
        jdbcTemplate.update("DELETE FROM subjects WHERE subject = 'ctc-map-summer'");
        jdbcTemplate.update("DELETE FROM acl WHERE acl_id IN (SELECT acl_id FROM site_file WHERE file_path = 'ctc-map')");
        jdbcTemplate.update("DELETE FROM site_file WHERE file_path = 'ctc-map'");
        jdbcTemplate.update("DELETE FROM gps_location WHERE country = 'ctc-map'");
    }

    @Test
    void clustersMergeTheReadableSiteFilesAtALowZoomAndShowPointsAtStreetLevel() throws Exception {
        mockMvc.perform(post("/map/clusters").header("Authorization", adminBearerToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"south\": 59, \"west\": 20, \"north\": 63, \"east\": 30, \"zoom\": 3}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.precision").value(1))
            .andExpect(jsonPath("$.clustered").value(true))
            .andExpect(jsonPath("$.total_points").value(4))
            .andExpect(jsonPath("$.clusters", hasSize(2)))
            .andExpect(jsonPath("$.clusters[0].count").value(3))
            .andExpect(jsonPath("$.clusters[0].cell_latitude").value(60.2))
            .andExpect(jsonPath("$.clusters[1].count").value(1))
            .andExpect(jsonPath("$.clusters[1].file_id").value(tampere))
            .andExpect(jsonPath("$.clusters[1].filename").value("map_tampere.jpg"));

        mockMvc.perform(post("/map/clusters").header("Authorization", adminBearerToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"south\": 60.1, \"west\": 24.8, \"north\": 60.3, \"east\": 25.1, \"zoom\": 18, \"tags\": [\"CTC-MAP-SUMMER\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clustered").value(false))
            .andExpect(jsonPath("$.total_points").value(1))
            .andExpect(jsonPath("$.clusters[0].file_id").value(helsinkiB));

        mockMvc.perform(post("/map/clusters").header("Authorization", adminBearerToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"south\": 60.1, \"west\": 24.8, \"north\": 60.3, \"east\": 25.1, \"zoom\": 18, \"file_type\": \"IMAGE\", \"search\": \"helsinki\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clusters[*].file_id").value(containsInAnyOrder((int) helsinkiA, (int) helsinkiB)));
    }

    @Test
    void theFilesOfAClusterCarryTheirThumbnailWhichTheThumbnailEndpointServes() throws Exception {
        mockMvc.perform(post("/map/files").header("Authorization", adminBearerToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"cell_latitude\": 60.2, \"cell_longitude\": 24.9, \"precision\": 1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].id").value(helsinkiVideo))
            .andExpect(jsonPath("$[0].thumbnail_id").isEmpty())
            .andExpect(jsonPath("$[2].id").value(helsinkiA))
            .andExpect(jsonPath("$[2].filename").value("map_helsinki_a.jpg"))
            .andExpect(jsonPath("$[2].file_type").value("IMAGE"))
            .andExpect(jsonPath("$[2].thumbnail_id").value(thumbId));

        // the thumbnail of a readable site file is served, an unreadable one is refused, an unknown one is not found
        mockMvc.perform(get("/content-management/file/site-files/{id}/thumbnail", helsinkiA).header("Authorization", adminBearerToken()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_JPEG))
            .andExpect(content().bytes(new byte[]{ (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9 }));
        mockMvc.perform(get("/content-management/file/site-files/{id}/thumbnail", denied).header("Authorization", adminBearerToken()))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/content-management/file/site-files/{id}/thumbnail", 987654321L).header("Authorization", adminBearerToken()))
            .andExpect(status().isNotFound());
        // a video without a thumbnail has nothing to show
        mockMvc.perform(get("/content-management/file/site-files/{id}/thumbnail", helsinkiVideo).header("Authorization", adminBearerToken()))
            .andExpect(status().isNotFound());
    }

    private long seedSiteFile(String fileName, FileTypeEnum fileType, String mimeType, double latitude, double longitude, String takenAt, boolean readable)
            throws Exception {
        // gps_location ids come from the file backend (the upstream ids of the ingest), so the test assigns its own
        var location = gpsLocationRepository.save(GpsLocation.builder()
            .id(nextLocationId++)
            .latitude(BigDecimal.valueOf(latitude))
            .latitudeRef('N')
            .longitude(BigDecimal.valueOf(longitude))
            .longitudeRef('E')
            .country("ctc-map")
            .build());
        // the administrator (user 1) owns the readable files; the unreadable one belongs to user 2 only
        // the administrator (user 1) owns the readable files; the unreadable one grants read to a user that is not the caller (the rows are
        // written directly, the ACL service would insist on an existing user account)
        var aclId = aclService.getNextAclId();
        jdbcTemplate.update("INSERT INTO acl (acl_id, user_id, unit_id, create_privilege, read_privilege, modify_privilege, delete_privilege) "
                + "VALUES (?, ?, NULL, TRUE, TRUE, TRUE, TRUE)", aclId, readable ? 1L : 987654L);
        var siteFile = siteFileRepository.save(SiteFile.builder()
            .aclId(aclId)
            .creator(1L)
            .created(Instant.now())
            .fileId(900_000L + aclId)
            .fileName(fileName)
            .filePath("ctc-map")
            .mimeType(mimeType)
            .size(10L)
            .fileType(fileType)
            .sha256sum("ctc-map-" + fileName)
            .originalDateTime(Instant.parse(takenAt))
            .location(location)
            .build());
        return siteFile.getId();
    }

    private String adminBearerToken() {
        return "Bearer "
                + jwtUtils.generateJwtTokenForUser("Vempain Administrator", "admin", "admin@nohost.nodomain")
                    .getTokenString();
    }
}
