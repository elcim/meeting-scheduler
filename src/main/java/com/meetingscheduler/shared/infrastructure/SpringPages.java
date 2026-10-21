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

    public static Pageable toPageable(PageQuery query) {
        return PageRequest.of(query.page(), query.size());
    }

    public static <E, T> Page<T> toPage(org.springframework.data.domain.Page<E> page, Function<E, T> mapper) {
        return new Page<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
