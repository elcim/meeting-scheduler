package com.meetingscheduler.shared.api;

import com.meetingscheduler.shared.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Representation of one page of results.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /**
     * Converts a domain page, mapping each of its elements.
     *
     * @param page the page to convert
     * @param mapper converts each element
     */
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.content().stream().map(mapper).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
