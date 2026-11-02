package fi.poltsi.vempain.admin.service.file;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.repository.file.FileThumbPageableRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.common.api.FileTypeEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * The thumbnail image of a site file for the previews of the admin SPA: the thumbnail {@code FileThumbService} generated under the site file storage
 * ({@code <site-file-directory>/<thumb filepath>/<filename>}), or the site file itself when it is an image without a thumbnail. Needs the read privilege on the
 * site file.
 */
@Slf4j
@Service
public class SiteFileThumbnailService {

    private final SiteFileRepository siteFileRepository;
    private final FileThumbPageableRepository fileThumbPageableRepository;
    private final AccessService accessService;
    private final String siteFileDirectory;

    public SiteFileThumbnailService(SiteFileRepository siteFileRepository, FileThumbPageableRepository fileThumbPageableRepository, AccessService accessService,
            @Value("${vempain.admin.file.site-file-directory}") String siteFileDirectory) {
        this.siteFileRepository = siteFileRepository;
        this.fileThumbPageableRepository = fileThumbPageableRepository;
        this.accessService = accessService;
        this.siteFileDirectory = siteFileDirectory;
    }

    /** The image to show for the site file and its MIME type */
    public record Thumbnail(Resource resource, String mimeType) {}

    /**
     * @throws ResponseStatusException 404 when the site file does not exist or has nothing to show, 403 without the read privilege
     */
    public Thumbnail thumbnailOf(long siteFileId) {
        var siteFile = siteFileRepository.findById(siteFileId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND));
        if (!accessService.hasReadPermission(siteFile.getAclId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, VempainMessages.UNAUTHORIZED_ACCESS);
        }

        var thumb = fileThumbPageableRepository.findFileThumbByParentId(siteFileId)
            .map(fileThumb -> Path.of(siteFileDirectory + File.separator + fileThumb.getFilepath() + File.separator + fileThumb.getFilename()))
            .filter(Files::isReadable);
        if (thumb.isPresent()) {
            return new Thumbnail(new FileSystemResource(thumb.get()), "image/jpeg");
        }

        // No generated thumbnail: an image is shown as it is, anything else has no preview
        if (siteFile.getFileType() == FileTypeEnum.IMAGE) {
            var original = Path.of(siteFileDirectory + File.separator + siteFile.getFileType().shortName + File.separator + siteFile.getFilePath()
                    + File.separator + siteFile.getFileName());
            if (Files.isReadable(original)) {
                return new Thumbnail(new FileSystemResource(original), Optional.ofNullable(siteFile.getMimeType())
                    .orElse("image/jpeg"));
            }
        }
        log.debug("Site file {} has no thumbnail to show", siteFileId);
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND);
    }
}
