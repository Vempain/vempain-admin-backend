package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.entity.Unit;
import fi.poltsi.vempain.admin.repository.AclRepository;
import fi.poltsi.vempain.admin.repository.UnitRepository;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class UnitServiceUTC {
    private final static int count = 10;

    @Mock
    UnitRepository unitRepository;
	@Mock
	AclRepository  aclRepository;

    private UnitService unitService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        unitService = new UnitService(unitRepository, aclRepository);
    }

    @Test
    void findAllOk() {
        List<Unit> units = TestUTCTools.generateUnitList(count);
        when(unitRepository.findAll()).thenReturn(units);

        try {
            Iterable<Unit> results = unitService.findAll();
            assertEquals(10L, StreamSupport.stream(results.spliterator(), false).count());
        } catch (Exception e) {
            fail("Should not have received an exception when fetching all units: " + e.getMessage());
        }
    }

    @Test
    void findByIdOk() {
        var unit = TestUTCTools.generateUnit(1L);
        when(unitRepository.findById(anyLong())).thenReturn(Optional.of(unit));

        try {
            var unitResponse = unitService.findById(1L);
            assertNotNull(unitResponse);
            assertEquals(1L, unitResponse.getId());
        } catch (Exception e) {
            fail("Should not have received an exception when fetching a unit " + e.getMessage());
        }
    }

    @Test
    void deleteById() {
        doNothing().when(unitRepository).deleteUnitById(1L);

        try {
            unitService.deleteById(1L);
        } catch (Exception e) {
            fail("Should not have received an exception when deleting a unit by ID " + e.getMessage());
        }
    }
}
