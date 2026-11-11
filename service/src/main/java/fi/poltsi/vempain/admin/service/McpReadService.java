package fi.poltsi.vempain.admin.service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import fi.poltsi.vempain.admin.api.response.AdminStatisticsResponse;
import fi.poltsi.vempain.admin.api.response.ComponentResponse;
import fi.poltsi.vempain.admin.api.response.DataSummaryResponse;
import fi.poltsi.vempain.admin.api.response.FormResponse;
import fi.poltsi.vempain.admin.api.response.LayoutResponse;
import fi.poltsi.vempain.admin.api.response.McpPagedResponse;
import fi.poltsi.vempain.admin.api.response.PageResponse;
import fi.poltsi.vempain.admin.api.response.PublishScheduleResponse;
import fi.poltsi.vempain.admin.api.response.TagResponse;
import fi.poltsi.vempain.admin.api.response.file.GalleryResponse;
import fi.poltsi.vempain.admin.api.response.file.SiteFileResponse;
import fi.poltsi.vempain.admin.api.site.response.McpWebSitePageResponse;
import fi.poltsi.vempain.admin.entity.Form;
import fi.poltsi.vempain.admin.entity.PublishSchedule;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.entity.file.GalleryFile;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.repository.ComponentRepository;
import fi.poltsi.vempain.admin.repository.DataRepository;
import fi.poltsi.vempain.admin.repository.FormRepository;
import fi.poltsi.vempain.admin.repository.LayoutRepository;
import fi.poltsi.vempain.admin.repository.PageRepository;
import fi.poltsi.vempain.admin.repository.PublishScheduleRepository;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.repository.file.SubjectRepository;
import fi.poltsi.vempain.admin.service.file.GalleryFileService;
import fi.poltsi.vempain.auth.api.response.AclResponse;
import fi.poltsi.vempain.auth.api.response.UnitResponse;
import fi.poltsi.vempain.auth.api.response.UserResponse;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.auth.service.UnitService;
import fi.poltsi.vempain.auth.service.UserService;
import fi.poltsi.vempain.site.entity.WebSitePage;
import fi.poltsi.vempain.site.repository.WebSitePageRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class McpReadService {

    private final PageRepository pageRepository;
    private final PageService pageService;
    private final GalleryRepository galleryRepository;
    private final GalleryFileService galleryFileService;
    private final SiteFileRepository siteFileRepository;
    private final DataRepository dataRepository;
    private final LayoutRepository layoutRepository;
    private final ComponentRepository componentRepository;
    private final FormRepository formRepository;
    private final FormService formService;
    private final PublishScheduleRepository publishScheduleRepository;
    private final SubjectRepository subjectRepository;
    private final UserService userService;
    private final UnitService unitService;
    private final AclService aclService;
    private final WebSitePageRepository webSitePageRepository;
    private final StatisticsService statisticsService;

    @Transactional(readOnly = true)
    public McpPagedResponse<PageResponse> pages(int page, int size) {
        return mapped(pageRepository.findAll(pageable(page, size)), pageService::populateResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse page(long id) {
        var entity = pageRepository.findById(Long.valueOf(id))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found"));
        return pageService.populateResponse(entity);
    }

    @Transactional(readOnly = true)
    public McpPagedResponse<GalleryResponse> galleries(int page, int size) {
        return mapped(galleryRepository.findAll(pageable(page, size)), this::galleryResponse);
    }

    @Transactional(readOnly = true)
    public GalleryResponse gallery(long id) {
        var gallery = galleryRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gallery not found"));
        return galleryResponse(gallery);
    }

    @Transactional(readOnly = true)
    public McpPagedResponse<SiteFileResponse> siteFiles(int page, int size) {
        return mapped(siteFileRepository.findAll(pageable(page, size)), SiteFile::toResponse);
    }

    @Transactional(readOnly = true)
    public McpPagedResponse<DataSummaryResponse> dataSets(int page, int size) {
        return mapped(dataRepository.findAll(pageable(page, size)), entity -> {
            entity.setAcls(aclService.findAclByAclId(entity.getAclId()));
            return entity.toDataSummaryResponse();
        });
    }

    @Transactional(readOnly = true)
    public McpPagedResponse<LayoutResponse> layouts(int page, int size) {
        return mapped(layoutRepository.findAll(pageable(page, size)), layout -> {
            var response = layout.getLayoutResponse();
            response.setAcls(aclService.getAclResponses(layout.getAclId()));
            return response;
        });
    }

    @Transactional(readOnly = true)
    public McpPagedResponse<ComponentResponse> components(int page, int size) {
        return mapped(componentRepository.findAll(pageable(page, size)), component -> {
            var response = component.getComponentResponse();
            response.setAcls(aclService.getAclResponses(component.getAclId()));
            return response;
        });
    }

    @Transactional(readOnly = true)
    public McpPagedResponse<FormResponse> forms(int page, int size) {
        return mapped(formRepository.findAll(pageable(page, size)), this::formResponse);
    }

    @Transactional(readOnly = true)
    public McpPagedResponse<PublishScheduleResponse> schedules(int page, int size) {
        return mapped(publishScheduleRepository.findAll(pageable(page, size)), PublishSchedule::toResponse);
    }

    @Transactional(readOnly = true)
    public McpPagedResponse<TagResponse> tags(int page, int size) {
        return mapped(subjectRepository.findAll(pageable(page, size)), subject -> TagResponse.builder()
            .id(subject.getId())
            .tagName(subject.getSubjectName())
            .build());
    }

    @Transactional(readOnly = true)
    public McpPagedResponse<UserResponse> users(int page, int size) {
        var responses = StreamSupport.stream(userService.findAll()
            .spliterator(), false)
            .map(this::userResponse)
            .toList();
        return paged(responses, page, size);
    }

    @Transactional(readOnly = true)
    public McpPagedResponse<UnitResponse> units(int page, int size) {
        var responses = StreamSupport.stream(unitService.findAll()
            .spliterator(), false)
            .map(this::unitResponse)
            .toList();
        return paged(responses, page, size);
    }

    @Transactional(readOnly = true)
    public McpPagedResponse<AclResponse> acls(int page, int size) {
        var responses = aclService.findAll()
            .stream()
            .map(Acl::toResponse)
            .toList();
        return paged(responses, page, size);
    }

    @Transactional(readOnly = true)
    public McpPagedResponse<McpWebSitePageResponse> publishedPages(int page, int size) {
        return mapped(webSitePageRepository.findAll(pageable(page, size)), McpReadService::sitePageResponse);
    }

    @Transactional(readOnly = true)
    public AdminStatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }

    static void validatePagination(int page, int size) {
        if (page < 0
            || size < 1
            || size > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be non-negative and size must be between 1 and 500");
        }
    }

    private static PageRequest pageable(int page, int size) {
        validatePagination(page, size);
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
    }

    private static <S, T> McpPagedResponse<T> mapped(Page<S> source, Function<S, T> mapper) {
        return new McpPagedResponse<>(source.getContent()
            .stream()
            .map(mapper)
            .toList(), source.getNumber(), source.getSize(), source.getTotalElements());
    }

    private static <T> McpPagedResponse<T> paged(List<T> source, int page, int size) {
        validatePagination(page, size);
        var from = (int) Math.min((long) page * size, source.size());
        var to = Math.min(from + size, source.size());
        return new McpPagedResponse<>(source.subList(from, to), page, size, source.size());
    }

    private GalleryResponse galleryResponse(Gallery gallery) {
        gallery.setAcls(aclService.findAclByAclId(gallery.getAclId()));
        var files = galleryFileService.findGalleryFileByGalleryId(gallery.getId())
            .stream()
            .map(GalleryFile::getSiteFileId)
            .map(siteFileRepository::findById)
            .flatMap(java.util.Optional::stream)
            .toList();
        gallery.setSiteFiles(files);
        return gallery.getResponse();
    }

    private FormResponse formResponse(Form form) {
        try {
            return formService.getFormResponse(form);
        } catch (fi.poltsi.vempain.admin.exception.VempainComponentException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not build form response", e);
        }
    }

    private UserResponse userResponse(UserAccount user) {
        var response = user.getUserResponse();
        response.setUnitIds(user.getUnits()
            .stream()
            .map(Unit::getId)
            .sorted()
            .toList());
        response.setAcls(aclService.getAclResponses(user.getAclId()));
        return response;
    }

    private UnitResponse unitResponse(Unit unit) {
        var response = unit.getUnitResponse();
        unitService.populateWithMembers(response);
        response.setAcls(aclService.getAclResponses(unit.getAclId()));
        return response;
    }

    private static McpWebSitePageResponse sitePageResponse(WebSitePage page) {
        return McpWebSitePageResponse.builder()
            .id(page.getId())
            .aclId(page.getAclId())
            .pageId(page.getPageId())
            .parentId(page.getParentId())
            .filePath(page.getFilePath())
            .secure(page.isSecure())
            .indexList(page.isIndexList())
            .title(page.getTitle())
            .header(page.getHeader())
            .body(page.getBody())
            .pageStyle(page.getPageStyle())
            .creator(page.getCreator())
            .created(page.getCreated())
            .modifier(page.getModifier())
            .modified(page.getModified())
            .published(page.getPublished())
            .build();
    }
}
