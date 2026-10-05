package fi.poltsi.vempain.admin.schedule;

import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.entity.Form;
import fi.poltsi.vempain.admin.entity.Layout;
import fi.poltsi.vempain.admin.entity.Page;
import fi.poltsi.vempain.admin.service.ComponentService;
import fi.poltsi.vempain.admin.service.FormService;
import fi.poltsi.vempain.admin.service.LayoutService;
import fi.poltsi.vempain.admin.service.PageService;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.auth.service.UnitService;
import fi.poltsi.vempain.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AclConsistencyScheduleUTC {
	@Mock
	private AclService       aclService;
	@Mock
	private ComponentService componentService;
	@Mock
	private FormService      formService;
	@Mock
	private LayoutService    layoutService;
	@Mock
	private PageService      pageService;
	@Mock
	private UnitService      unitService;
	@Mock
	private UserService      userService;
	@Mock
	private FileService      fileService;

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
}
