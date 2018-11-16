package ru.ifmo.rain.lisicyna.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPClient implements HelloClient {
    private class Handler implements Runnable {
        private String prefix;
        private int requests;
        private InetSocketAddress addr;
        private int port;

        public Handler(String host, int port, String prefix, int number, int requests) {
            this.prefix = prefix + number + "_";
            this.requests = requests;
            this.port = port;
            addr = new InetSocketAddress(host, port);
        }

        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket()) {
                //socket.setSoTimeout(100);
                int i = 1;
                byte[] inputData = new byte[socket.getReceiveBufferSize()];
                while (i <= requests) {
                    String prefixi = prefix + i;
                    DatagramPacket request = new DatagramPacket(prefixi.getBytes("UTF-8"), 0, prefixi.length(), addr);
                    socket.send(request);
                    DatagramPacket answer = new DatagramPacket(inputData, 0, inputData.length, addr);
                    try {
                        socket.receive(answer);
                        byte[] input = answer.getData();
                        String stringAnswer = new String(input, 0, answer.getLength());
                        if (stringAnswer.equals("Hello, " + prefixi)) {
                            System.out.println(stringAnswer);
                            i++;
                        }
                    }catch (SocketTimeoutException e) {
                        System.out.println("Sorry, we have some problems with time(");
                    } catch (IOException e) {
                        System.out.println("Sorry, we have some problems with receiving(");
                    }
                }
            } catch (IOException e) {
                System.out.println("Sorry, we have some problems with socket(");
            }
        }
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 1; i <= threads; i++) {
            pool.submit(new Handler(host, port, prefix, i, requests));
        }
        pool.shutdown();
        try {
            pool.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            pool.shutdown();
        }
    }
}
