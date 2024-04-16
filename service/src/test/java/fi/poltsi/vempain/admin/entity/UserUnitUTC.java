package fi.poltsi.vempain.admin.entity;

import fi.poltsi.vempain.admin.tools.TestUTCTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserUnitUTC {
    private UserUnit userUnit;

    @BeforeEach
    void setUp() {
        userUnit = TestUTCTools.generateUserUnit(1L, 1L);
        assertNotNull(userUnit);
    }

    @Test
    void testToStringOk() {
        assertTrue(!userUnit.toString().isEmpty());
    }

    @Test
    void getIdOk() {
        UserUnitId userUnitId = userUnit.getId();
        assertNotNull(userUnitId);
        assertEquals(1L, userUnitId.getUserId());
        assertEquals(1L, userUnitId.getUnitId());
    }

    @Test
    void getUserOk() {
        assertEquals(1L, userUnit.getUser().getId());
    }

    @Test
    void getUnitOk() {
        assertEquals(1L, userUnit.getUnit().getId());
    }

    @Test
    void testEqualsOk() {
        UserUnit clone = TestUTCTools.deepCopy(userUnit, UserUnit.class);
        boolean equals = userUnit.equals(clone);
        assertTrue(equals);
    }

    @Test
    void canEqualOk() {
        UserUnit clone = TestUTCTools.deepCopy(userUnit, UserUnit.class);
        assertTrue(userUnit.canEqual(clone));
    }

    @Test
    void testHashCodeOk() {
        UserUnit clone = TestUTCTools.deepCopy(userUnit, UserUnit.class);
        assertNotNull(clone);
        assertEquals(userUnit.hashCode(), clone.hashCode());
    }

    @Test
    void setIdOk() {
        UserUnitId newId = TestUTCTools.generateUserUnitId(2L, 2L);
        userUnit.setId(newId);
        assertEquals(newId, userUnit.getId());
    }

    @Test
    void setUserOk() {
        User newUser = TestUTCTools.generateUser(2L);
        userUnit.setUser(newUser);
        assertEquals(newUser, userUnit.getUser());
    }

    @Test
    void setUnitOk() {
        Unit newUnit = TestUTCTools.generateUnit(2L);
        userUnit.setUnit(newUnit);
        assertEquals(newUnit, userUnit.getUnit());
    }

    @Test
    void builderOk() {
        assertEquals(1L, userUnit.getUser().getId());
        assertEquals(1L, userUnit.getUnit().getId());
    }
}
