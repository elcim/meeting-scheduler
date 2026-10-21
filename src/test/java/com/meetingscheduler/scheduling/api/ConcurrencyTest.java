package com.meetingscheduler.scheduling.api;

import com.meetingscheduler.shared.api.ApiTestSupport;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrencyTest extends ApiTestSupport {

    private static final int THREADS = 16;

    @Test
    void onlyOneOfManyConcurrentBookingsOfTheSameSlotSucceeds() throws Exception {
        long user = createUser();
        long slot = createSlotId(user, "2030-10-01T09:00:00Z", 30);

        List<Integer> statuses = runConcurrently(i ->
                bookMeeting(user, slot, "Meeting " + i).andReturn().getResponse().getStatus());

        assertThat(statuses).containsOnly(201, 409);
        assertThat(statuses.stream().filter(s -> s == 201)).hasSize(1);
    }

    @Test
    void onlyOneOfManyConcurrentOverlappingSlotsIsCreated() throws Exception {
        long user = createUser();

        List<Integer> statuses = runConcurrently(i ->
                createSlot(user, "2030-10-02T09:00:00Z", 30 + i).andReturn().getResponse().getStatus());

        assertThat(statuses).containsOnly(201, 409);
        assertThat(statuses.stream().filter(s -> s == 201)).hasSize(1);
    }

    private List<Integer> runConcurrently(Task task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                int index = i;
                Callable<Integer> call = () -> {
                    start.await();
                    return task.run(index);
                };
                futures.add(executor.submit(call));
            }
            start.countDown();
            List<Integer> results = new ArrayList<>();
            for (Future<Integer> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private interface Task {
        int run(int index) throws Exception;
    }
}
