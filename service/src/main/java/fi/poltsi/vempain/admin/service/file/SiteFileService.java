package fi.poltsi.vempain.admin.service.file;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteFileService {

    private final SiteFileRepository siteFileRepository;

    @Transactional
    public SiteFile save(SiteFile siteFile) {
        return siteFileRepository.save(siteFile);
    }
}
