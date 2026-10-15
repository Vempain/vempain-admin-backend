package fi.poltsi.vempain.admin.exception;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertNull;

@Slf4j
class DataObjectAlreadyExistsExceptionUTC {

    @Test
    void throwOk() {
        try {
            throw new DataObjectAlreadyExistsException();
        } catch (DataObjectAlreadyExistsException e) {
            log.info("Exception: ", e);
            assertNull(e.getMessage());
        }
    }
}
