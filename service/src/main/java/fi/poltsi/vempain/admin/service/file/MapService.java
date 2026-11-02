package fi.poltsi.vempain.admin.service.file;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.common.api.request.MapClusterFilesRequest;
import fi.poltsi.vempain.common.api.request.MapClusterRequest;
import fi.poltsi.vempain.common.api.response.MapClusterResponse;
import fi.poltsi.vempain.common.api.response.MapFileResponse;
import fi.poltsi.vempain.common.map.MapClusterQuery;
import fi.poltsi.vempain.common.map.MapQuerySchema;
import lombok.extern.slf4j.Slf4j;

/**
 * The map of the published site files with GPS coordinates: the shared clustering query of {@code vempain-common-core} over this backend's {@code site_file}
 * and {@code gps_location} tables, the subjects (tags) of the files and their thumbnails ({@code file_thumb}), filtered to the site files the caller may read.
 */
@Slf4j
@Service
public class MapService {

    /** Where this backend keeps its GPS-tagged site files, for the shared query */
    static final MapQuerySchema SCHEMA = MapQuerySchema.builder()
        .fileTable("site_file")
        .idColumn("id")
        .aclIdColumn("acl_id")
        .filenameColumn("file_name")
        .filePathColumn("file_path")
        .fileTypeColumn("file_type")
        .mimetypeColumn("mime_type")
        .datetimeColumn("original_datetime")
        .locationIdColumn("location_id")
        .locationTable("gps_location")
        .tagLinkTable("file_subject")
        .tagLinkFileColumn("site_file_id")
        .tagLinkTagColumn("subject_id")
        .tagTable("subjects")
        .tagNameColumn("subject")
        // the thumbnail row of the site file; the admin SPA shows it through GET /content-management/file/site-files/{id}/thumbnail
        .thumbnailSubquery("(SELECT MIN(t.id) FROM file_thumb t WHERE t.parent_id = p.id)")
        .build();

    private final MapClusterQuery query;
    private final AccessService accessService;

    public MapService(@Qualifier("adminDataSource") DataSource adminDataSource, AccessService accessService) {
        this.query = new MapClusterQuery(new NamedParameterJdbcTemplate(adminDataSource), SCHEMA);
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public MapClusterResponse clusters(MapClusterRequest request) {
        var response = query.clusters(request, accessService.mapPrincipal());
        log.debug("Map clusters at zoom {} (precision {}): {} clusters, {} site files", request.getZoom(), response.getPrecision(), response.getClusters()
            .size(), response.getTotalPoints());
        return response;
    }

    @Transactional(readOnly = true)
    public List<MapFileResponse> files(MapClusterFilesRequest request) {
        return query.files(request, accessService.mapPrincipal());
    }
}
