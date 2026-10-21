package com.meetingscheduler.shared.infrastructure;

import com.meetingscheduler.shared.domain.Page;
import com.meetingscheduler.shared.domain.PageQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.function.Function;

/**
 * Translates between the domain's paging types and Spring Data's, for the persistence adapters.
 */
public final class SpringPages {

    private SpringPages() {
    }

    /**
     * The Spring Data counterpart of a page query.
     *
     * @param query index and size of the page
     */
    public static Pageable toPageable(PageQuery query) {
        return PageRequest.of(query.page(), query.size());
    }

    /**
     * Converts a Spring Data page into a domain page, mapping each of its elements.
     *
     * @param page the Spring Data page to convert
     * @param mapper converts each element
     */
    public static <E, T> Page<T> toPage(org.springframework.data.domain.Page<E> page, Function<E, T> mapper) {
        return new Page<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
