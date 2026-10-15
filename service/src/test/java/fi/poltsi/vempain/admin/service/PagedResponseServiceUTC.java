package fi.poltsi.vempain.admin.service;

import java.util.List;

import org.junit.jupiter.api.Test;

import fi.poltsi.vempain.auth.api.request.PagedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagedResponseServiceUTC {

    private static PagedRequest request(int page, int size) {
        var request = new PagedRequest();
        request.setPage(page);
        request.setSize(size);
        return request;
    }

    @Test
    void slicesTheItemsOfTheRequestedPage() {
        var items = List.of("a", "b", "c", "d", "e");

        var first = PagedResponseService.create(items, request(0, 2));
        assertEquals(List.of("a", "b"), first.getContent());
        assertEquals(5, first.getTotalElements());
        assertEquals(3, first.getTotalPages());
        assertTrue(first.isFirst());
        assertFalse(first.isLast());

        var last = PagedResponseService.create(items, request(2, 2));
        assertEquals(List.of("e"), last.getContent());
        assertFalse(last.isFirst());
        assertTrue(last.isLast());
    }

    @Test
    void aPageBeyondTheItemsIsEmptyAndLast() {
        var response = PagedResponseService.create(List.of("a", "b"), request(5, 2));

        assertTrue(response.getContent()
            .isEmpty());
        assertEquals(1, response.getTotalPages());
        assertTrue(response.isLast());
    }

    @Test
    void aZeroSizeHasNoPagesAndNoContent() {
        var response = PagedResponseService.create(List.of("a", "b"), request(0, 0));

        assertTrue(response.getContent()
            .isEmpty());
        assertEquals(0, response.getTotalPages());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }
}
