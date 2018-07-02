package ru.ifmo.rain.lisicyna.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import ru.ifmo.rain.lisicyna.mapper.ParallelMapperImpl;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {
    private ParallelMapper mapper;

    public IterativeParallelism() {
        mapper = null;
    }

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    private <T> List<Stream<? extends T>> splitList(int threads, List<? extends T> values) {
        int sizeSubList = Math.max(Math.floorDiv(values.size(), threads), 1);
        int left = 0, right = sizeSubList + values.size() % threads;
        List<Stream<? extends T>> result = new ArrayList<>();
        while (left < values.size()) {
            result.add(values.subList(left, Math.min(right, values.size())).stream());
            left = right;
            right += sizeSubList;
        }
        return result;
    }

    private <T, R> List<R> task(List<Stream<? extends T>> values, Function<Stream<? extends T>, ? extends R> job) throws InterruptedException {
        List<R> resultList;
        if (mapper != null) {
            resultList = mapper.map(job, values);
        } else {
            List<Thread> threadList = new ArrayList<>();
            resultList = new ArrayList<>(Collections.nCopies(values.size(), null));
            for (int i = 0; i < values.size(); i++) {
                final int position = i;
                Thread thread = new Thread(() -> {
                    resultList.set(position, job.apply(values.get(position)));
                });
                threadList.add(thread);
                thread.start();
            }
            for (Thread thread : threadList) {
                thread.join();
            }
        }
        return resultList;
    }

    private <T> T taskWithComparator(List<Stream<? extends T>> values, Function<Stream<? extends T>, ? extends T> job) throws  InterruptedException{
        return job.apply(task(values, job).stream());
    }

    private <T, R> R taskWithoutComparator(List<Stream<? extends T>> values, Function<Stream<? extends T>, ? extends R> job, Function<Stream<? extends R>, ? extends R> finalJob) throws InterruptedException{
        return finalJob.apply(task(values, job).stream());
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return taskWithComparator(splitList(threads, values), (stream) -> {return stream.max(comparator).get();});
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return taskWithComparator(splitList(threads, values), (stream) -> {return stream.min(comparator).get();});
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return taskWithoutComparator(splitList(threads, values), (stream) -> {return stream.allMatch(predicate);}, (stream) -> {return stream.allMatch(s -> {return s.equals(true);});});
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return taskWithoutComparator(splitList(threads, values), (stream) -> {return stream.anyMatch(predicate);}, (stream) -> {return stream.anyMatch(s -> {return s.equals(true);});});
    }

    @Override
    public String join(int threads, List<? extends Object> values) throws InterruptedException {
        return taskWithoutComparator(splitList(threads, values), (stream) -> {return stream.map(Object::toString).collect(Collectors.joining());},(stream) -> {return stream.collect(Collectors.joining());});
        //    return null;
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return taskWithoutComparator(splitList(threads, values), (stream) -> {return stream.filter(predicate).collect(Collectors.toList());}, (stream) -> {return stream.flatMap(Collection::stream).collect(Collectors.toList());});
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return taskWithoutComparator(splitList(threads, values), (stream) -> {return stream.map(f).collect(Collectors.toList());}, (stream) -> {return stream.flatMap(Collection::stream).collect(Collectors.toList());});
    }
}
