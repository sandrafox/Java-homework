package ru.ifmo.rain.lisicyna.crawler;

import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WebCrawler implements Crawler {
    private int downloaders;
    private Downloader downloader;
    private AtomicInteger extractors;
    private List<String> downloaded;
    private Map<String, IOException> errors;
    private Map<String, Integer> depthOfURL;
    private BlockingQueue<String> urlForDownload;
    Lock lock;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = downloaders;
        this.extractors = new AtomicInteger(extractors);
        lock = new ReentrantLock();
    }

    private void dfsIteration() {
        try {
            String url = urlForDownload.take();
            lock.lock();
            try {

                if (!downloaded.contains(url)) {
                    downloaded.add(url);
                    Document document = downloader.download(url);
                    if (extractors.addAndGet(-1) > 0) {
                        for (String s : document.extractLinks()) {

                        }
                    }
                }
            } catch (IOException e) {
                errors.put(url, e);
                downloaded.remove(url);
            }
            lock.unlock();
        } catch (InterruptedException e) {
            System.out.println("Sorry, problem with taking element from BlockingQueue");
        }
    }

    @Override
    public Result download(String url, int depth) {
        downloaded = new ArrayList<>();
        errors = new ConcurrentHashMap<>();
        depthOfURL = new ConcurrentHashMap<>();
        urlForDownload = new ArrayBlockingQueue<String>(depth * extractors.get());
        try {
            urlForDownload.put(url);
        } catch (InterruptedException e) {
            System.out.println("Sorry, problem with BlockingQueue");
        }
        
        return new Result(downloaded, errors);
    }

    @Override
    public void close() {

    }
}
