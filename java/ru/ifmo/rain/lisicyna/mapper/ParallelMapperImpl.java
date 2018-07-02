package ru.ifmo.rain.lisicyna.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private class Result<T> {
        private List<T> result;
        private int count;

        public Result(int count) {
            result = new ArrayList<>(Collections.nCopies(count, null));
        }

        public void set(int position, T element) {
            result.set(position, element);
            synchronized (this) {
                count++;
                if (count == result.size()) {
                    notify();
                }
            }
        }

        public synchronized List<T> getResult() throws InterruptedException {
            while (count < result.size()) {
                wait();
            }
            return result;
        }
    }

    private Queue<Runnable> jobs;
    private List<Thread> workers;
    final private int maxSize = 10000000;

    private synchronized void addJob(Runnable job) throws InterruptedException {
        while (jobs.size() == maxSize) {
            wait();
        }
        jobs.add(job);
        notify();
    }

    public ParallelMapperImpl(int threads) {
        jobs = new ArrayDeque<>();
        workers = new ArrayList<>(threads);
        for (int i = 0; i < threads; i++) {
            Thread worker = new Thread(() -> {
                try {
                    Runnable job;
                    while (!Thread.interrupted()) {
                        synchronized (this) {
                            while (jobs.isEmpty()) {
                                wait();
                            }
                            job = jobs.poll();
                            notify();
                        }
                        job.run();
                    }
                } catch (InterruptedException e) {
                } finally {
                    Thread.currentThread().interrupt();
                }
            });
            workers.add(worker);
            worker.start();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        Result<R> result = new Result<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            final int position = i;
            addJob(() -> {
                result.set(position, f.apply(args.get(position)));
            });
        }
        return result.getResult();
    }

    @Override
    public void close() {
        for (Thread thread : workers) {
            thread.interrupt();
        }
        for (Thread thread : workers) {
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
    }
}
