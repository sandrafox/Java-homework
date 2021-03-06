package ru.ifmo.rain.lisicyna.concurentrudenko;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ScalarIP {
    private final ParallelMapper mapper;

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    public IterativeParallelism() {
        mapper = null;
    }

    private <T, R> R baseTask(int threads, final List<? extends T> values,
                              final Function<? super Stream<? extends T>, ? extends R> task,
                              final Function<? super Stream<? extends R>, ? extends R> ansCollector)
            throws InterruptedException {
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads must be positive");
        }

        threads = Math.max(1, Math.min(threads, values.size()));
        final List<Stream<? extends T>> subTasks = new ArrayList<>();
        final int blockSize = values.size() / threads;
        int rest = values.size() % threads;
        int pr = 0;
        for (int i = 0; i < threads; i++) {
            final int l = pr;
            final int r = l + blockSize + (rest-- > 0 ? 1 : 0);
            pr = r;
            subTasks.add(values.subList(l, r).stream());
        }

        List<R> res;
        if (mapper != null) {
            res = mapper.map(task, subTasks);
        } else {
            final List<Thread> workers = new ArrayList<>();
            res = new ArrayList<>(Collections.nCopies(threads, null));
            for (int i = 0; i < threads; i++) {
                final int pos = i;
                addAndStart(workers, new Thread(() -> res.set(pos, task.apply(subTasks.get(pos)))));
            }
            joinThreads(workers);
        }
        return ansCollector.apply(res.stream());
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Unable to handle empty list");
        }
        final Function<Stream<? extends T>, ? extends T> streamMax = stream -> stream.max(comparator).get();
        return baseTask(threads, values, streamMax, streamMax);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, Collections.reverseOrder(comparator));
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return baseTask(threads, values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::booleanValue)
        );
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, elem -> !predicate.test(elem));
    }

    private void addAndStart(List<Thread> workers, Thread thread) {
        workers.add(thread);
        thread.start();
    }

    private void joinThreads(final List<Thread> threads) throws InterruptedException {
        InterruptedException exception = null;
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                if (exception == null) {
                    exception = new InterruptedException("Not all threads joined");
                }
                exception.addSuppressed(e);
            }
        }
        if (exception != null) {
            throw exception;
        }
    }
}