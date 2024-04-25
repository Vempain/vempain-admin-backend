package fi.poltsi.vempain.admin.schedule;

import fi.poltsi.vempain.admin.entity.Acl;
import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.entity.Form;
import fi.poltsi.vempain.admin.entity.Layout;
import fi.poltsi.vempain.admin.entity.Page;
import fi.poltsi.vempain.admin.entity.Unit;
import fi.poltsi.vempain.admin.entity.User;
import fi.poltsi.vempain.admin.service.AclService;
import fi.poltsi.vempain.admin.service.ComponentService;
import fi.poltsi.vempain.admin.service.FormService;
import fi.poltsi.vempain.admin.service.LayoutService;
import fi.poltsi.vempain.admin.service.PageService;
import fi.poltsi.vempain.admin.service.UnitService;
import fi.poltsi.vempain.admin.service.UserService;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

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

    private AclConsistencySchedule aclConsistencySchedule;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aclConsistencySchedule = new AclConsistencySchedule(aclService, componentService, formService, layoutService, pageService,
                                                            unitService, userService, fileService,
                                                            new HashSet<>(), new HashSet<>(), new HashSet<>(), new ArrayList<>());
    }

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
        List<Acl> acls = new ArrayList<>();

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
        components.getFirst().setAclId(1L);
        components.getFirst().setAclId(2L);
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
        forms.get(0).setAclId(2L);
        forms.get(1).setAclId(2L);
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
        layouts.get(0).setAclId(3L);
        layouts.get(1).setAclId(3L);
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
        pages.get(0).setAclId(4L);
        pages.get(1).setAclId(4L);
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
        units.get(0).setAclId(5L);
        units.get(1).setAclId(5L);
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
        List<User> users = TestUTCTools.generateUserList(2L);
        users.get(0).setAclId(6L);
        users.get(1).setAclId(6L);
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
        forms.getFirst().setAclId(2L);
        when(formService.findAll()).thenReturn(forms);

        List<Layout> layouts = TestUTCTools.generateLayoutList(1L);
        layouts.getFirst().setAclId(3L);
        when(layoutService.findAll()).thenReturn(layouts);

        List<Page> pages = TestUTCTools.generatePageList(1L);
        pages.getFirst().setAclId(4L);
        when(pageService.findAll()).thenReturn(pages);

        List<Unit> units = TestUTCTools.generateUnitList(1L);
        units.getFirst().setAclId(5L);
        when(unitService.findAll()).thenReturn(units);

        List<User> users = TestUTCTools.generateUserList(1L);
        users.getFirst().setAclId(6L);
        when(userService.findAll()).thenReturn(users);
    }
}
