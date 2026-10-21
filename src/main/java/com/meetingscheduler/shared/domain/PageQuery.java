package com.meetingscheduler.shared.domain;

/**
 * Which page of a result to fetch: a zero-based page index and the page size.
 */
public record PageQuery(int page, int size) {
}
