package fi.poltsi.vempain.admin.api.response;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import fi.poltsi.vempain.admin.api.request.LayoutRequest;
import fi.poltsi.vempain.auth.api.response.AclResponse;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutResponseUTC {

    @Test
    void layoutRequestCarriesTheIdNameStructureAndEveryAcl() {
        var userAcl = AclResponse.builder()
            .id(10L)
            .aclId(5L)
            .user(2L)
            .readPrivilege(true)
            .modifyPrivilege(true)
            .build();
        var unitAcl = AclResponse.builder()
            .id(11L)
            .aclId(5L)
            .unit(3L)
            .readPrivilege(true)
            .createPrivilege(true)
            .deletePrivilege(true)
            .build();
        var response = LayoutResponse.builder()
            .id(7L)
            .layoutName("Landing page")
            .structure("<!--comp_1-->")
            .acls(List.of(userAcl, unitAcl))
            .locked(false)
            .creator(1L)
            .modifier(1L)
            .created(Instant.parse("2026-08-15T12:00:00Z"))
            .modified(Instant.parse("2026-08-15T12:00:00Z"))
            .build();

        LayoutRequest request = response.getLayoutRequest();

        assertEquals(7L, request.getId());
        assertEquals("Landing page", request.getLayoutName());
        assertEquals("<!--comp_1-->", request.getStructure());
        assertEquals(2, request.getAcls()
            .size());
        var first = request.getAcls()
            .getFirst();
        assertEquals(10L, first.getId());
        assertEquals(5L, first.getAclId());
        assertEquals(2L, first.getUser());
        assertNull(first.getUnit());
        assertTrue(first.isReadPrivilege());
        assertTrue(first.isModifyPrivilege());
        assertFalse(first.isCreatePrivilege());
        assertFalse(first.isDeletePrivilege());
        var second = request.getAcls()
            .get(1);
        assertEquals(3L, second.getUnit());
        assertNull(second.getUser());
        assertTrue(second.isCreatePrivilege());
        assertTrue(second.isDeletePrivilege());
        assertFalse(second.isModifyPrivilege());
    }

    @Test
    void layoutRequestOfAResponseWithoutAclsIsEmpty() {
        var response = LayoutResponse.builder()
            .id(8L)
            .layoutName("Bare")
            .structure("")
            .acls(List.of())
            .build();

        LayoutRequest request = response.getLayoutRequest();

        assertEquals(8L, request.getId());
        assertTrue(request.getAcls()
            .isEmpty());
    }

    @Test
    void theDerivedRequestIsNotPartOfTheJsonContract() throws Exception {
        var response = LayoutResponse.builder()
            .id(9L)
            .layoutName("Hidden")
            .structure("x")
            .acls(List.of())
            .build();

        String json = JsonMapper.builder()
            .build()
            .writeValueAsString(response);

        assertFalse(json.contains("layout_request"), json);
        assertTrue(json.contains("\"layout_name\":\"Hidden\""), json);
    }
}
