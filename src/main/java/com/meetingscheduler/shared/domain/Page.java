package com.meetingscheduler.shared.domain;

import java.util.List;
import java.util.function.Function;

/**
 * One page of a query result, independent of any persistence framework.
 */
public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        return new Page<>(content.stream().<R>map(mapper).toList(), page, size, totalElements, totalPages);
    }
}
