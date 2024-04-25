package fi.poltsi.vempain.admin.entity;

import fi.poltsi.vempain.admin.api.response.UnitResponse;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnitUTC {
    private Unit unit;

    @BeforeEach
    void setUp() {
        unit = TestUTCTools.generateUnit(1L);
    }

    @Test
    void getUnitResponseOk() {
        UnitResponse unitResponse = unit.getUnitResponse();
        assertEquals(1L, unitResponse.getId());
    }

    @Test
    void builderOk() {
        assertEquals(1L, unit.getId());
        assertEquals("Test unit 1", unit.getName());
    }

    @Test
    void testToStringOk() {
		assertFalse(unit.toString()
						.isEmpty());
    }

    @Test
    void getDescriptionOk() {
        unit.setDescription("Description");
        assertEquals("Description", unit.getDescription());
    }

    @Test
    void testEqualsOk() {
        Unit clone = TestUTCTools.deepCopy(unit, Unit.class);
        boolean equals = unit.equals(clone);
        assertTrue(equals);
    }

    @Test
    void canEqualOk() {
        Unit clone = TestUTCTools.deepCopy(unit, Unit.class);
        assertTrue(unit.canEqual(clone));
    }

    @Test
    void testHashCodeOk() {
        Unit clone = TestUTCTools.deepCopy(unit, Unit.class);
        assertNotNull(clone);
        assertEquals(unit.hashCode(), clone.hashCode());
    }
}
