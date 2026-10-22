package fi.poltsi.vempain.site.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.site.entity.WebSiteSubject;
import fi.poltsi.vempain.site.repository.SiteSubjectRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSiteSubjectServiceUTC {

    @Mock
    private SiteSubjectRepository siteSubjectRepository;

    @InjectMocks
    private WebSiteSubjectService webSiteSubjectService;

    private Subject buildSubject(long id, String name) {
        return Subject.builder()
            .id(id)
            .subjectName(name)
            .subjectNameDe("de-" + name)
            .subjectNameEn("en-" + name)
            .subjectNameEs("es-" + name)
            .subjectNameFi("fi-" + name)
            .subjectNameSe("se-" + name)
            .build();
    }

    private WebSiteSubject buildSiteSubject(long id, String name) {
        return WebSiteSubject.builder()
            .id(id)
            .subjectName(name)
            .subjectNameDe("de-" + name)
            .subjectNameEn("en-" + name)
            .subjectNameEs("es-" + name)
            .subjectNameFi("fi-" + name)
            .subjectNameSe("se-" + name)
            .build();
    }

    @Test
    void saveFromAdminSubjectNewOk() {
        var subject = buildSubject(1L, "nature");
        var saved = buildSiteSubject(10L, "nature");

        when(siteSubjectRepository.findBySubjectName("nature")).thenReturn(null);
        when(siteSubjectRepository.save(any(WebSiteSubject.class))).thenReturn(saved);

        var result = webSiteSubjectService.saveFromAdminSubject(subject, "alice");

        assertNotNull(result);
        assertEquals("nature", result.getSubjectName());
        var captor = ArgumentCaptor.forClass(WebSiteSubject.class);
        verify(siteSubjectRepository).save(captor.capture());
        // A new site subject records who published it and when
        assertEquals("alice", captor.getValue()
            .getCreator());
        assertNotNull(captor.getValue()
            .getCreated());
        assertNull(captor.getValue()
            .getModifier());
        assertNull(captor.getValue()
            .getModified());
    }

    @Test
    void saveFromAdminSubjectExistingUpdatesOk() {
        var subject = buildSubject(1L, "nature");
        var existing = buildSiteSubject(10L, "nature");
        existing.setSubjectNameEn("old-en");

        when(siteSubjectRepository.findBySubjectName("nature")).thenReturn(existing);
        when(siteSubjectRepository.save(any(WebSiteSubject.class))).thenReturn(existing);

        var result = webSiteSubjectService.saveFromAdminSubject(subject, "bob");

        assertNotNull(result);
        assertEquals("en-nature", result.getSubjectNameEn());
        verify(siteSubjectRepository).save(existing);
        // An existing site subject keeps its creator and records the update
        assertNull(existing.getCreator());
        assertEquals("bob", existing.getModifier());
        assertNotNull(existing.getModified());
    }

    @Test
    void saveAllFromAdminSubjectOk() {
        var subjects = List.of(buildSubject(1L, "nature"), buildSubject(2L, "travel"));
        var savedNature = buildSiteSubject(10L, "nature");
        var savedTravel = buildSiteSubject(11L, "travel");

        when(siteSubjectRepository.findBySubjectName("nature")).thenReturn(null);
        when(siteSubjectRepository.findBySubjectName("travel")).thenReturn(null);
        when(siteSubjectRepository.save(any(WebSiteSubject.class))).thenReturn(savedNature)
            .thenReturn(savedTravel);

        var result = webSiteSubjectService.saveAllFromAdminSubject(subjects, "alice");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void saveAllFromAdminSubjectEmptyOk() {
        var result = webSiteSubjectService.saveAllFromAdminSubject(List.of(), "alice");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void saveSiteFileSubjectOk() {
        var subject1 = buildSiteSubject(10L, "nature");
        var subject2 = buildSiteSubject(11L, "travel");

        doNothing().when(siteSubjectRepository)
            .saveSiteFileSubject(anyLong(), anyLong());

        webSiteSubjectService.saveSiteFileSubject(100L, List.of(subject1, subject2));

        verify(siteSubjectRepository, times(2)).saveSiteFileSubject(anyLong(), anyLong());
    }

    @Test
    void saveSiteFileSubjectEmptyListOk() {
        webSiteSubjectService.saveSiteFileSubject(100L, List.of());

        verify(siteSubjectRepository, times(0)).saveSiteFileSubject(anyLong(), anyLong());
    }
}
