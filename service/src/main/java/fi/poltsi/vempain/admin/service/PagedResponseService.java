package fi.poltsi.vempain.admin.service;

import java.util.List;

import fi.poltsi.vempain.auth.api.request.PagedRequest;
import fi.poltsi.vempain.auth.api.response.PagedResponse;

final class PagedResponseService {

    private PagedResponseService() {}

    static <T> PagedResponse<T> create(List<T> items, PagedRequest request) {
        int page = request.getPage();
        int size = request.getSize();
        long total = items.size();
        int pages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        int from = Math.min(page * size, items.size());
        int to = Math.min(from + size, items.size());
        return PagedResponse.of(items.subList(from, to), page, size, total, pages, page == 0, pages == 0
                                                                                              || page + 1 >= pages);
    }
}
