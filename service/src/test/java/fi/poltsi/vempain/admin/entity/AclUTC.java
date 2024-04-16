package fi.poltsi.vempain.admin.entity;

import fi.poltsi.vempain.admin.api.response.AclResponse;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AclUTC {

    @Test
    void getAclResponseOk() {
        Acl acl = TestUTCTools.generateAcl(1L, 1L, 1L, null);
        AclResponse aclResponse = acl.toResponse();
        assertEquals(acl.getPermissionId(), aclResponse.getPermissionId());
        assertEquals(acl.getAclId(), aclResponse.getAclId());
        assertEquals(acl.getUserId(), aclResponse.getUser());
        assertEquals(acl.getUnitId(), aclResponse.getUnit());
        assertEquals(acl.isReadPrivilege(), aclResponse.isReadPrivilege());
        assertEquals(acl.isModifyPrivilege(), aclResponse.isModifyPrivilege());
        assertEquals(acl.isCreatePrivilege(), aclResponse.isCreatePrivilege());
        assertEquals(acl.isDeletePrivilege(), aclResponse.isDeletePrivilege());
    }

    @Test
    void testToStringOk() {
        Acl acl = TestUTCTools.generateAcl(1L, 1L, 1L, null);
		assertFalse(acl.toString().isEmpty());
    }

    @Test
    void testEqualsOk() {
        Acl acl = TestUTCTools.generateAcl(1L, 1L, 1L, null);
        Acl aclCopy = TestUTCTools.deepCopy(acl, Acl.class);
        boolean equals = acl.equals(aclCopy);
        assertTrue(equals);
    }

    @Test
    void canEqualOk() {
        Acl acl = TestUTCTools.generateAcl(1L, 1L, 1L, null);
        Acl aclCopy = TestUTCTools.deepCopy(acl, Acl.class);
        assertTrue(acl.canEqual(aclCopy));
    }

    @Test
    void testHashCodeOk() {
        Acl acl = TestUTCTools.generateAcl(1L, 1L, 1L, null);
        Acl aclCopy = TestUTCTools.deepCopy(acl, Acl.class);
        assertNotNull(aclCopy);
        assertEquals(acl.hashCode(), aclCopy.hashCode());
    }

    @Test
    void setPermissionIdOk() {
        Acl acl = TestUTCTools.generateAcl(1L, 1L, 1L, null);
        acl.setPermissionId(666L);
        assertEquals(666L, acl.getPermissionId());
    }

    @Test
    void setAclIdOk() {
        Acl acl = TestUTCTools.generateAcl(1L, 1L, 1L, null);
        acl.setAclId(666L);
        assertEquals(666L, acl.getAclId());
    }

    @Test
    void setUserIdOk() {
        Acl acl = TestUTCTools.generateAcl(1L, 1L, 1L, null);
        acl.setUserId(666L);
        assertEquals(666L, acl.getUserId());
    }

    @Test
    void setUnitIdOk() {
        Acl acl = TestUTCTools.generateAcl(1L, 1L, 1L, null);
        acl.setUnitId(666L);
        assertEquals(666L, acl.getUnitId());
    }

    @Test
    void setCreatePrivOk() {
        Acl acl = TestUTCTools.generateAcl(1L, 1L, 1L, null);
        acl.setCreatePrivilege(false);
		assertFalse(acl.isCreatePrivilege());
    }

    @Test
    void setReadPrivOk() {
        Acl acl = TestUTCTools.generateAcl(1L, 1L, 1L, null);
        acl.setReadPrivilege(false);
		assertFalse(acl.isReadPrivilege());
    }

    @Test
    void setModifyPrivOk() {
        Acl acl = TestUTCTools.generateAcl(1L, 1L, 1L, null);
        acl.setModifyPrivilege(false);
		assertFalse(acl.isModifyPrivilege());
    }

    @Test
    void setDeletePrivOk() {
        Acl acl = TestUTCTools.generateAcl(1L, 1L, 1L, null);
        acl.setDeletePrivilege(false);
		assertFalse(acl.isDeletePrivilege());
    }

    @Test
    void builderOk() {
        Acl acl = Acl.builder()
                     .permissionId(1L)
                     .aclId(2L)
                     .unitId(null)
                     .createPrivilege(true)
                     .readPrivilege(true)
                     .modifyPrivilege(true)
                     .deletePrivilege(true)
                     .build();
        assertEquals(1L, acl.getPermissionId());
        assertEquals(2L, acl.getAclId());
        assertNull(acl.getUnitId());
		assertTrue(acl.isCreatePrivilege());
		assertTrue(acl.isReadPrivilege());
		assertTrue(acl.isModifyPrivilege());
		assertTrue(acl.isDeletePrivilege());
    }
}
