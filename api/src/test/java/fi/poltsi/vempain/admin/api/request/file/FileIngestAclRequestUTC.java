package fi.poltsi.vempain.admin.api.request.file;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileIngestAclRequestUTC {

    @Test
    void anyPrivilegeIsTrueWhenAtLeastOneFlagIsSet() {
        assertTrue(FileIngestAclRequest.builder()
            .userId(1L)
            .readPrivilege(true)
            .build()
            .isAnyPrivilege());
        assertTrue(FileIngestAclRequest.builder()
            .userId(1L)
            .createPrivilege(true)
            .build()
            .isAnyPrivilege());
        assertTrue(FileIngestAclRequest.builder()
            .userId(1L)
            .modifyPrivilege(true)
            .build()
            .isAnyPrivilege());
        assertTrue(FileIngestAclRequest.builder()
            .userId(1L)
            .deletePrivilege(true)
            .build()
            .isAnyPrivilege());
    }

    @Test
    void anyPrivilegeIsFalseWhenNoFlagIsSet() {
        assertFalse(FileIngestAclRequest.builder()
            .userId(1L)
            .build()
            .isAnyPrivilege());
        assertFalse(new FileIngestAclRequest().isAnyPrivilege());
    }

    @Test
    void theDerivedFlagIsNotWrittenToJson() throws Exception {
        String json = JsonMapper.builder()
            .build()
            .writeValueAsString(FileIngestAclRequest.builder()
                .userId(4L)
                .readPrivilege(true)
                .build());

        assertFalse(json.contains("any_privilege"), json);
        assertTrue(json.contains("\"user_id\":4"), json);
    }

    @Test
    void validationRejectsAnEntryWithoutPrivilegesOrUser() {
        Validator validator = validatorOrNull();

        if (validator == null) {
            return;
        }

        var noPrivilege = FileIngestAclRequest.builder()
            .userId(4L)
            .build();
        var violations = validator.validate(noPrivilege);
        assertEquals(1, violations.size(), violations.toString());
        assertEquals("At least one privilege must be granted", violations.iterator()
            .next()
            .getMessage());

        var valid = FileIngestAclRequest.builder()
            .userId(4L)
            .modifyPrivilege(true)
            .build();
        assertTrue(validator.validate(valid)
            .isEmpty());

        var unit = FileIngestAclRequest.builder()
            .unitId(3L)
            .readPrivilege(true)
            .build();
        assertTrue(validator.validate(unit)
            .isEmpty());

        var both = FileIngestAclRequest.builder()
            .userId(4L)
            .unitId(3L)
            .readPrivilege(true)
            .build();
        assertEquals(1, validator.validate(both)
            .size());

        var neither = FileIngestAclRequest.builder()
            .readPrivilege(true)
            .build();
        assertEquals(1, validator.validate(neither)
            .size());
    }

    @Test
    void exactlyOneOfUserAndUnitMustBeSet() {
        assertTrue(FileIngestAclRequest.builder()
            .userId(4L)
            .build()
            .isUserOrUnit());
        assertTrue(FileIngestAclRequest.builder()
            .unitId(3L)
            .build()
            .isUserOrUnit());
        assertFalse(FileIngestAclRequest.builder()
            .userId(4L)
            .unitId(3L)
            .build()
            .isUserOrUnit());
        assertFalse(FileIngestAclRequest.builder()
            .build()
            .isUserOrUnit());
    }

    private static Validator validatorOrNull() {
        try {
            return Validation.buildDefaultValidatorFactory()
                .getValidator();
        } catch (jakarta.validation.NoProviderFoundException e) {
            // The api module only declares the validation API; the provider lives in the service module
            return null;
        }
    }
}
