package fi.poltsi.vempain.admin.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fi.poltsi.vempain.admin.api.request.file.FileIngestAclRequest;
import fi.poltsi.vempain.auth.api.AccountStatus;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.auth.service.UnitService;
import fi.poltsi.vempain.auth.service.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The grantee handling shared by the file ingest and the data sets. The validation and the additive grant are also exercised through
 * {@code FileIngestServiceUTC}; this class covers the replacing variant used by data set updates and the request-shaped read-back.
 */
@ExtendWith(MockitoExtension.class)
class AclGranteeServiceUTC {

    private static final long ACL_ID = 10L;
    private static final long OWNER = 1L;

    @Mock
    private AclService aclService;
    @Mock
    private UserService userService;
    @Mock
    private UnitService unitService;

    @InjectMocks
    private AclGranteeService service;

    @Test
    void replaceRemovesUnlistedGranteesKeepsTheOwnerAndUpsertsTheListedOnes() throws Exception {
        var ownerRow = row(1L, OWNER, null, true, true, true, true);
        var staleUser = row(2L, 5L, null, true, false, false, false);
        var keptUnit = row(3L, null, 3L, true, false, false, false);
        when(aclService.findAclByAclId(ACL_ID)).thenReturn(List.of(ownerRow, staleUser, keptUnit));
        when(aclService.save(any(Acl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.replace(ACL_ID, List.of(unitAcl(3L, true, true), userAcl(7L, true, false)), OWNER);

        // user 5 is no longer listed and goes, the owner's row is never touched
        verify(aclService).deleteById(2L);
        verify(aclService, never()).deleteById(1L);
        // unit 3 stays and gets the requested privileges, user 7 is new
        assertTrue(keptUnit.isModifyPrivilege());
        verify(aclService).update(keptUnit);
        verify(aclService).save(argThat(acl -> Long.valueOf(7L)
            .equals(acl.getUserId())
                                               && acl.getAclId() == ACL_ID
                                               && acl.isReadPrivilege()
                                               && !acl.isModifyPrivilege()));
    }

    @Test
    void replaceWithAnEmptyListRemovesEveryAdditionalGranteeAndNullLeavesTheAclAlone() {
        var ownerRow = row(1L, OWNER, null, true, true, true, true);
        var extra = row(2L, 5L, null, true, false, false, false);
        when(aclService.findAclByAclId(ACL_ID)).thenReturn(List.of(ownerRow, extra));

        service.replace(ACL_ID, List.of(), OWNER);
        verify(aclService).deleteById(2L);
        verify(aclService, never()).deleteById(1L);

        service.replace(ACL_ID, null, OWNER);
        service.replace(0L, List.of(), OWNER);
        verify(aclService, org.mockito.Mockito.times(1)).findAclByAclId(ACL_ID);
    }

    @Test
    void additionalGranteesLeavesOutTheOwnerAndAnswersRequestShapedRows() {
        when(aclService.findAclByAclId(ACL_ID)).thenReturn(List.of(row(1L, OWNER, null, true, true, true, true), row(2L, 5L, null, true, false, true, false),
                row(3L, null, 3L, false, false, false, true)));

        var grantees = service.additionalGrantees(ACL_ID, OWNER);

        assertEquals(2, grantees.size());
        assertEquals(5L, grantees.get(0)
            .getUserId());
        assertTrue(grantees.get(0)
            .isModifyPrivilege());
        assertEquals(3L, grantees.get(1)
            .getUnitId());
        assertTrue(grantees.get(1)
            .isDeletePrivilege());
        assertTrue(service.additionalGrantees(0L, OWNER)
            .isEmpty());
    }

    @Test
    void validateAcceptsActiveUsersAndExistingUnitsOnce() {
        var active = new UserAccount();
        active.setId(5L);
        active.setStatus(AccountStatus.ACTIVE);
        when(userService.findById(5L)).thenReturn(Optional.of(active));
        var unit = new Unit();
        unit.setId(3L);
        when(unitService.findAll()).thenReturn(List.of(unit));

        service.validate(List.of(userAcl(5L, true, false), unitAcl(3L, true, false)));
        service.validate(null);
        service.validate(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.validate(List.of(userAcl(5L, true, false), userAcl(5L, false, true))));
        assertThrows(IllegalArgumentException.class, () -> service.validate(List.of(unitAcl(4L, true, false))));
        assertThrows(IllegalArgumentException.class, () -> service.validate(List.of(userAcl(5L, false, false))));
    }

    private static Acl row(long id, Long userId, Long unitId, boolean read, boolean create, boolean modify, boolean delete) {
        var acl = Acl.builder()
            .aclId(ACL_ID)
            .userId(userId)
            .unitId(unitId)
            .readPrivilege(read)
            .createPrivilege(create)
            .modifyPrivilege(modify)
            .deletePrivilege(delete)
            .build();
        acl.setId(id);
        return acl;
    }

    private static FileIngestAclRequest userAcl(long userId, boolean read, boolean modify) {
        return FileIngestAclRequest.builder()
            .userId(userId)
            .readPrivilege(read)
            .modifyPrivilege(modify)
            .build();
    }

    private static FileIngestAclRequest unitAcl(long unitId, boolean read, boolean modify) {
        return FileIngestAclRequest.builder()
            .unitId(unitId)
            .readPrivilege(read)
            .modifyPrivilege(modify)
            .build();
    }
}
