package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.tools.TestUTCTools;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDetailsImplUTC {

    @Test
    void testEqualsOk() {
		var unit = TestUTCTools.generateUnit(1);
        UserDetailsImpl userDetails1 = new UserDetailsImpl(1L, "login", "nick", "email@address", "password", Set.of(unit),null);
        UserDetailsImpl userDetails2 = new UserDetailsImpl(1L, "login", "nick", "email@address", "password", Set.of(unit), null);
        boolean equals = userDetails1.equals(userDetails2);
        assertTrue(equals);
    }

    @Test
    void testEqualsSelfOk() {
        UserDetailsImpl userDetails1 = new UserDetailsImpl(1L, "login", "nick", "email@address", "password", null, null);
        boolean equals = userDetails1.equals(userDetails1);
        assertTrue(equals);
    }

    @Test
    void testEqualsNullFail() {
        UserDetailsImpl userDetails1 = new UserDetailsImpl(1L, "login", "nick", "email@address", "password", null, null);
        boolean equals = userDetails1.equals(null);
        assertFalse(equals);
    }

    @Test
    void testEqualsOtherClassFail() {
        UserDetailsImpl userDetails1 = new UserDetailsImpl(1L, "login", "nick", "email@address", "password", null, null);
        RuntimeException re = new RuntimeException();
        boolean equals = userDetails1.equals(re);
        assertFalse(equals);
    }

    @Test
    void hashCodeOk() {
        UserDetailsImpl userDetails1 = new UserDetailsImpl(1L, "login", "nick", "email@address", "password", null, null);
        assertTrue(userDetails1.hashCode() > 0);
    }
}
