package fi.poltsi.vempain.admin.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpReadServiceUTC {

    @Test
    void paginationRequiresNonNegativePageAndSizeFromOneThroughFiveHundred() {
        assertThrows(ResponseStatusException.class, () -> McpReadService.validatePagination(-1, 10));
        assertThrows(ResponseStatusException.class, () -> McpReadService.validatePagination(0, 0));
        assertThrows(ResponseStatusException.class, () -> McpReadService.validatePagination(0, 501));
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class, () -> McpReadService.validatePagination(0, 501)).getStatusCode());

        McpReadService.validatePagination(0, 1);
        McpReadService.validatePagination(7, 500);
    }
}
