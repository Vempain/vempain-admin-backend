package fi.poltsi.vempain.admin.schedule;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.entity.Form;
import fi.poltsi.vempain.admin.entity.Layout;
import fi.poltsi.vempain.admin.entity.Page;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.exception.VempainLayoutException;
import fi.poltsi.vempain.admin.service.ComponentService;
import fi.poltsi.vempain.admin.service.FormService;
import fi.poltsi.vempain.admin.service.LayoutService;
import fi.poltsi.vempain.admin.service.PageService;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import fi.poltsi.vempain.auth.entity.AbstractVempainEntity;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.auth.service.UnitService;
import fi.poltsi.vempain.auth.service.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AclConsistencyScheduleUTC {

    @Mock
    private AclService aclService;
    @Mock
    private ComponentService componentService;
    @Mock
    private FormService formService;
    @Mock
    private LayoutService layoutService;
    @Mock
    private PageService pageService;
    @Mock
    private UnitService unitService;
    @Mock
    private UserService userService;
    @Mock
    private FileService fileService;

    @InjectMocks
    private AclConsistencySchedule aclConsistencySchedule;

    @Test
    void verifyOk() {
        testWithGivenAclTableCount(6L);
    }

    @Test
    void verifyTableAclMoreTableAclsThanObjectFail() {
        testWithGivenAclTableCount(10L);
    }

    @Test
    void verifyTableAclLessTableAclsThanObjectFail() {
        testWithGivenAclTableCount(3L);
    }

    private void testWithGivenAclTableCount(long tableCount) {
        var acls = new ArrayList<Acl>();

        for (long i = 1; i <= tableCount; i++) {
            acls.addAll(TestUTCTools.generateAclList(i, 2L));
        }

        when(aclService.findAll()).thenReturn(acls);

        setupSingleObjects();

        try {
            aclConsistencySchedule.verify();
        } catch (Exception e) {
            fail("Should not have received any exceptions on a successful run:" + e);
        }
    }

    @Test
    void getAclSetFromServicesOk() {
        setupSingleObjects();

        try {
            aclConsistencySchedule.getAclSetFromServices();
        } catch (Exception e) {
            fail("Should not have received any exceptions on a successful run");
        }
    }

    @Test
    void getAclSetFromServicesComponentAclFail() {
        setupSingleObjects();
        List<Component> components = TestUTCTools.generateComponentList(2L);
        components.getFirst()
            .setAclId(1L);
        components.getFirst()
            .setAclId(2L);
        when(componentService.findAll()).thenReturn(components);

        try {
            aclConsistencySchedule.getAclSetFromServices();
        } catch (Exception e) {
            fail("Should not have received any exceptions on a successful run");
        }
    }

    @Test
    void getAclSetFromServicesFormAclFail() {
        setupSingleObjects();
        List<Form> forms = TestUTCTools.generateFormList(2L);
        forms.get(0)
            .setAclId(2L);
        forms.get(1)
            .setAclId(2L);
        when(formService.findAll()).thenReturn(forms);

        try {
            aclConsistencySchedule.getAclSetFromServices();
        } catch (Exception e) {
            fail("Should not have received any exceptions on a successful run");
        }
    }

    @Test
    void getAclSetFromServicesLayoutAclFail() {
        setupSingleObjects();
        List<Layout> layouts = TestUTCTools.generateLayoutList(2L);
        layouts.get(0)
            .setAclId(3L);
        layouts.get(1)
            .setAclId(3L);
        when(layoutService.findAll()).thenReturn(layouts);

        try {
            aclConsistencySchedule.getAclSetFromServices();
        } catch (Exception e) {
            fail("Should not have received any exceptions on a successful run");
        }
    }

    @Test
    void getAclSetFromServicesPageAclFail() {
        setupSingleObjects();
        List<Page> pages = TestUTCTools.generatePageList(2L);
        pages.get(0)
            .setAclId(4L);
        pages.get(1)
            .setAclId(4L);
        when(pageService.findAll()).thenReturn(pages);

        try {
            aclConsistencySchedule.getAclSetFromServices();
        } catch (Exception e) {
            fail("Should not have received any exceptions on a successful run");
        }
    }

    @Test
    void getAclSetFromServicesUnitAclFail() {
        setupSingleObjects();
        List<Unit> units = TestUTCTools.generateUnitList(2L);
        units.get(0)
            .setAclId(5L);
        units.get(1)
            .setAclId(5L);
        when(unitService.findAll()).thenReturn(units);

        try {
            aclConsistencySchedule.getAclSetFromServices();
        } catch (Exception e) {
            fail("Should not have received any exceptions on a successful run");
        }
    }

    @Test
    void getAclSetFromServicesUserAclFail() {
        setupSingleObjects();
        List<UserAccount> users = TestUTCTools.generateUserList(2L);
        users.get(0)
            .setAclId(6L);
        users.get(1)
            .setAclId(6L);
        when(userService.findAll()).thenReturn(users);

        try {
            aclConsistencySchedule.getAclSetFromServices();
        } catch (Exception e) {
            fail("Should not have received any exceptions on a successful run");
        }
    }

    private void setupSingleObjects() {
        List<Component> components = TestUTCTools.generateComponentList(1L);
        when(componentService.findAll()).thenReturn(components);

        List<Form> forms = TestUTCTools.generateFormList(1L);
        forms.getFirst()
            .setAclId(2L);
        when(formService.findAll()).thenReturn(forms);

        List<Layout> layouts = TestUTCTools.generateLayoutList(1L);
        layouts.getFirst()
            .setAclId(3L);
        when(layoutService.findAll()).thenReturn(layouts);

        List<Page> pages = TestUTCTools.generatePageList(1L);
        pages.getFirst()
            .setAclId(4L);
        when(pageService.findAll()).thenReturn(pages);

        List<Unit> units = TestUTCTools.generateUnitList(1L);
        units.getFirst()
            .setAclId(5L);
        when(unitService.findAll()).thenReturn(units);

        List<UserAccount> users = TestUTCTools.generateUserList(1L);
        users.getFirst()
            .setAclId(6L);
        when(userService.findAll()).thenReturn(users);
    }

    @Test
    void verifyAssignsCreatorOwnedAclToEntitiesWithoutAcl() throws Exception {
        setupSingleObjects();
        var acls = new ArrayList<Acl>();
        for (long i = 1; i <= 6; i++) {
            acls.addAll(TestUTCTools.generateAclList(i, 2L));
        }
        when(aclService.findAll()).thenReturn(acls);

        var orphanComponent = TestUTCTools.generateComponentList(1L)
            .getFirst();
        orphanComponent.setAclId(0L);
        orphanComponent.setCreator(42L);
        var components = new ArrayList<Component>(TestUTCTools.generateComponentList(1L));
        components.add(orphanComponent);
        when(componentService.findAll()).thenReturn(components);
        when(aclService.createNewAcl(42L, null, true, true, true, true)).thenReturn(77L);

        aclConsistencySchedule.verify();

        verify(aclService).createNewAcl(42L, null, true, true, true, true);
        verify(componentService).save(orphanComponent);
        assertEquals(77L, orphanComponent.getAclId());
    }

    @Test
    void verifyAssignsOperatorOwnedAclWhenCreatorIsUnknown() throws Exception {
        setupSingleObjects();
        var acls = new ArrayList<Acl>();
        for (long i = 1; i <= 6; i++) {
            acls.addAll(TestUTCTools.generateAclList(i, 2L));
        }
        when(aclService.findAll()).thenReturn(acls);

        var siteFile = fi.poltsi.vempain.admin.entity.file.SiteFile.builder()
            .id(9L)
            .aclId(0L)
            .creator(null)
            .build();
        when(fileService.findAllSiteFiles()).thenReturn(List.of(siteFile));
        when(aclService.createNewAcl(1L, null, true, true, true, true)).thenReturn(78L);

        aclConsistencySchedule.verify();

        verify(fileService).saveSiteFile(siteFile);
        assertEquals(78L, siteFile.getAclId());
    }

    @Test
    void verifyLogsAndContinuesWhenAclCreationFails() throws Exception {
        setupSingleObjects();
        var acls = new ArrayList<Acl>();
        for (long i = 1; i <= 6; i++) {
            acls.addAll(TestUTCTools.generateAclList(i, 2L));
        }
        when(aclService.findAll()).thenReturn(acls);

        var orphanPage = TestUTCTools.generatePageList(1L)
            .getFirst();
        orphanPage.setAclId(0L);
        orphanPage.setCreator(5L);
        var pages = new ArrayList<Page>(TestUTCTools.generatePageList(1L));
        pages.getFirst()
            .setAclId(4L);
        pages.add(orphanPage);
        when(pageService.findAll()).thenReturn(pages);
        when(aclService.createNewAcl(5L, null, true, true, true, true)).thenThrow(new VempainAclException("Invalid user ID given in the ACL"));

        aclConsistencySchedule.verify();

        verify(pageService, never()).save(orphanPage);
        assertEquals(0L, orphanPage.getAclId());
    }

    private void setupMatchingAclTable() {
        var acls = new ArrayList<Acl>();
        for (long i = 1; i <= 6; i++) {
            acls.addAll(TestUTCTools.generateAclList(i, 2L));
        }
        when(aclService.findAll()).thenReturn(acls);
    }

    @Test
    void duplicateAclRowsAreRemovedAndASingleRowIsLeftAlone() {
        setupSingleObjects();
        setupMatchingAclTable();
        var first = TestUTCTools.generateAcl(100L, 1L, 1L, null);
        var copy = TestUTCTools.generateAcl(101L, 1L, 1L, null);
        when(aclService.findDuplicateAcls()).thenReturn(List.of(1L, 2L, 3L));
        when(aclService.findAclByAclId(1L)).thenReturn(List.of(first, copy));
        when(aclService.findAclByAclId(2L)).thenReturn(List.of(TestUTCTools.generateAcl(102L, 2L, 1L, null)));
        when(aclService.findAclByAclId(3L)).thenReturn(null);

        aclConsistencySchedule.verify();

        // Each of the two equal rows is removed once from the point of view of the other
        verify(aclService).deleteById(100L);
        verify(aclService).deleteById(101L);
        verify(aclService, never()).deleteById(102L);
    }

    @Test
    void orphanAclsAreRemovedAndARemovalFailureIsLogged() throws Exception {
        setupSingleObjects();
        var acls = new ArrayList<Acl>();
        for (long i = 1; i <= 8; i++) {
            acls.addAll(TestUTCTools.generateAclList(i, 1L));
        }
        when(aclService.findAll()).thenReturn(acls);
        org.mockito.Mockito.doNothing()
            .when(aclService)
            .deleteByAclId(7L);
        doThrow(new VempainEntityNotFoundException("No ACL", "acl")).when(aclService)
            .deleteByAclId(8L);

        aclConsistencySchedule.verify();

        verify(aclService).deleteByAclId(7L);
        verify(aclService).deleteByAclId(8L);
    }

    @Test
    void aclRowsThatNameBothAUserAndAUnitAreSplit() throws Exception {
        setupSingleObjects();
        setupMatchingAclTable();
        var mixed = TestUTCTools.generateAcl(200L, 1L, 5L, 9L);
        when(aclService.findAllUserUnitAcls()).thenReturn(List.of(mixed));

        aclConsistencySchedule.verify();

        verify(aclService).update(mixed);
        verify(aclService).save(argThat(acl -> acl.getUnitId() != null
                                               && acl.getUnitId() == 9L
                                               && acl.getUserId() == null
                                               && acl.getAclId() == 1L));
        verify(aclService).save(mixed);
        assertEquals(null, mixed.getUnitId());
    }

    @Test
    void failuresWhileSplittingMixedAclRowsAreLogged() throws Exception {
        setupSingleObjects();
        setupMatchingAclTable();
        var mixed = TestUTCTools.generateAcl(200L, 1L, 5L, 9L);
        when(aclService.findAllUserUnitAcls()).thenReturn(List.of(mixed));
        doThrow(new VempainAclException("update failed")).when(aclService)
            .update(any());
        doThrow(new VempainAclException("save failed")).when(aclService)
            .save(any());

        aclConsistencySchedule.verify();

        verify(aclService, times(2)).save(any());
    }

    @Test
    void objectsSharingAnAclGetACopyOfTheRowsUnderANewAclId() throws Exception {
        setupSingleObjects();
        setupMatchingAclTable();
        var components = new ArrayList<>(TestUTCTools.generateComponentList(1L));
        var duplicate = TestUTCTools.generateComponentList(1L)
            .getFirst();
        duplicate.setId(77L);
        duplicate.setAclId(components.getFirst()
            .getAclId());
        components.add(duplicate);
        when(componentService.findAll()).thenReturn(components);
        when(aclService.getNextAclId()).thenReturn(500L);
        when(aclService.findAclByAclId(duplicate.getAclId()))
            .thenReturn(List.of(TestUTCTools.generateAcl(1L, duplicate.getAclId(), 3L, null), TestUTCTools.generateAcl(2L, duplicate.getAclId(), null, 4L)));

        aclConsistencySchedule.verify();

        verify(aclService).save(argThat(acl -> acl.getAclId() == 500L
                                               && acl.getUserId() != null
                                               && acl.getUserId() == 3L));
        verify(aclService).save(argThat(acl -> acl.getAclId() == 500L
                                               && acl.getUnitId() != null
                                               && acl.getUnitId() == 4L));
        verify(componentService).save(duplicate);
        assertEquals(500L, duplicate.getAclId());
    }

    @Test
    void objectsSharingAnAclWithoutRowsGetAnOperatorOwnedAcl() throws Exception {
        setupSingleObjects();
        setupMatchingAclTable();
        var forms = new ArrayList<>(TestUTCTools.generateFormList(1L));
        forms.getFirst()
            .setAclId(2L);
        var duplicate = TestUTCTools.generateFormList(1L)
            .getFirst();
        duplicate.setId(78L);
        duplicate.setAclId(2L);
        forms.add(duplicate);
        when(formService.findAll()).thenReturn(forms);
        when(aclService.getNextAclId()).thenReturn(501L);
        when(aclService.findAclByAclId(2L)).thenReturn(List.of());

        aclConsistencySchedule.verify();

        verify(aclService).save(argThat(acl -> acl.getAclId() == 501L
                                               && acl.getUserId() == 1L
                                               && acl.isReadPrivilege()
                                               && acl.isDeletePrivilege()));
    }

    @Test
    void failuresWhileCopyingDuplicateAclsAreLogged() throws Exception {
        setupSingleObjects();
        setupMatchingAclTable();
        var layouts = new ArrayList<>(TestUTCTools.generateLayoutList(1L));
        layouts.getFirst()
            .setAclId(3L);
        var duplicate = TestUTCTools.generateLayoutList(1L)
            .getFirst();
        duplicate.setId(79L);
        duplicate.setAclId(3L);
        layouts.add(duplicate);
        var pages = new ArrayList<>(TestUTCTools.generatePageList(1L));
        pages.getFirst()
            .setAclId(4L);
        var duplicatePage = TestUTCTools.generatePageList(1L)
            .getFirst();
        duplicatePage.setId(80L);
        duplicatePage.setAclId(4L);
        pages.add(duplicatePage);
        when(layoutService.findAll()).thenReturn(layouts);
        when(pageService.findAll()).thenReturn(pages);
        when(aclService.getNextAclId()).thenReturn(502L);
        when(aclService.findAclByAclId(3L)).thenReturn(List.of(TestUTCTools.generateAcl(1L, 3L, 3L, null)));
        when(aclService.findAclByAclId(4L)).thenReturn(List.of());
        doThrow(new VempainAclException("save failed")).when(aclService)
            .save(any());
        doThrow(new VempainLayoutException("layout save failed")).when(layoutService)
            .save(duplicate);

        aclConsistencySchedule.verify();

        verify(layoutService).save(duplicate);
        verify(pageService, never()).save(duplicatePage);
    }

    @Test
    void everyEntityTypeIsSavedThroughItsOwnService() throws Exception {
        setupSingleObjects();
        setupMatchingAclTable();
        var gallery = Gallery.builder()
            .id(90L)
            .aclId(0L)
            .creator(1L)
            .build();
        var siteFile = SiteFile.builder()
            .id(91L)
            .aclId(0L)
            .creator(1L)
            .build();
        var units = new ArrayList<>(TestUTCTools.generateUnitList(1L));
        units.getFirst()
            .setAclId(5L);
        var unit = TestUTCTools.generateUnitList(1L)
            .getFirst();
        unit.setId(92L);
        unit.setAclId(0L);
        units.add(unit);
        var users = new ArrayList<>(TestUTCTools.generateUserList(1L));
        users.getFirst()
            .setAclId(6L);
        var user = TestUTCTools.generateUserList(1L)
            .getFirst();
        user.setId(93L);
        user.setAclId(0L);
        users.add(user);
        var forms = new ArrayList<>(TestUTCTools.generateFormList(1L));
        forms.getFirst()
            .setAclId(2L);
        var form = TestUTCTools.generateFormList(1L)
            .getFirst();
        form.setId(94L);
        form.setAclId(0L);
        forms.add(form);
        when(fileService.findAllGalleries()).thenReturn(List.of(gallery));
        when(fileService.findAllSiteFiles()).thenReturn(List.of(siteFile));
        when(unitService.findAll()).thenReturn(units);
        when(userService.findAll()).thenReturn(users);
        when(formService.findAll()).thenReturn(forms);
        when(aclService.createNewAcl(1L, null, true, true, true, true)).thenReturn(600L);

        aclConsistencySchedule.verify();

        verify(fileService).saveGallery(gallery);
        verify(fileService).saveSiteFile(siteFile);
        verify(unitService).save(unit);
        verify(userService).save(user);
        verify(formService).save(form);
        assertEquals(600L, gallery.getAclId());
        assertEquals(600L, user.getAclId());
    }

    @Test
    void anUnknownEntityTypeIsReportedAndAComponentSaveFailureIsLogged() throws Exception {
        setupSingleObjects();
        setupMatchingAclTable();
        var components = new ArrayList<Component>(TestUTCTools.generateComponentList(1L));
        var broken = TestUTCTools.generateComponentList(1L)
            .getFirst();
        broken.setId(95L);
        broken.setAclId(0L);
        broken.setCreator(2L);
        components.add(broken);
        when(componentService.findAll()).thenReturn(components);
        var unknown = new AbstractVempainEntity() {
        };
        unknown.setId(96L);
        unknown.setAclId(0L);
        unknown.setCreator(2L);
        when(fileService.findAllSiteFiles()).thenReturn(List.of());
        when(fileService.findAllGalleries()).thenAnswer(invocation -> List.of());
        when(unitService.findAll()).thenAnswer(invocation -> {
            var units = new ArrayList<AbstractVempainEntity>(TestUTCTools.generateUnitList(1L));
            units.getFirst()
                .setAclId(5L);
            units.add(unknown);
            @SuppressWarnings({ "unchecked", "rawtypes" })
            List<fi.poltsi.vempain.auth.entity.Unit> result = (List) units;
            return result;
        });
        when(aclService.createNewAcl(2L, null, true, true, true, true)).thenReturn(601L);
        doThrow(new VempainComponentException("component save failed")).when(componentService)
            .save(broken);

        aclConsistencySchedule.verify();

        verify(componentService).save(broken);
        assertEquals(601L, unknown.getAclId());
    }

    @Test
    void missingAclRowsAreCreatedForTheCreatorAndACreationFailureIsLogged() throws Exception {
        setupSingleObjects();
        var acls = new ArrayList<Acl>();
        for (long i = 1; i <= 5; i++) {
            acls.addAll(TestUTCTools.generateAclList(i, 2L));
        }
        when(aclService.findAll()).thenReturn(acls);
        var users = TestUTCTools.generateUserList(1L);
        users.getFirst()
            .setAclId(6L);
        users.getFirst()
            .setCreator(3L);
        when(userService.findAll()).thenReturn(users);
        doThrow(new VempainAclException("save failed")).when(aclService)
            .save(argThat(acl -> acl.getAclId() == 6L));

        aclConsistencySchedule.verify();

        verify(aclService).save(argThat(acl -> acl.getAclId() == 6L
                                               && acl.getUserId() == 3L
                                               && acl.isReadPrivilege()));
    }

    @Test
    void anEmptyAclTableAndMissingObjectListsAreTolerated() {
        when(aclService.findAll()).thenReturn(null);
        when(componentService.findAll()).thenReturn(null);
        when(formService.findAll()).thenReturn(List.of());
        when(layoutService.findAll()).thenReturn(List.of());
        when(pageService.findAll()).thenReturn(List.of());
        when(unitService.findAll()).thenReturn(List.of());
        when(userService.findAll()).thenReturn(List.of());

        aclConsistencySchedule.verify();

        verify(aclService).findDuplicateAcls();
    }

    @Test
    void nothingHappensWhenSchedulingIsDisabled() {
        org.springframework.test.util.ReflectionTestUtils.setField(aclConsistencySchedule, "schedulingEnabled", false);

        aclConsistencySchedule.verify();

        verify(aclService, never()).findAll();
    }
}
