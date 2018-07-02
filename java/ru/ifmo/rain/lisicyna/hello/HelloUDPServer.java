package ru.ifmo.rain.lisicyna.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class HelloUDPServer implements HelloServer {
    private  boolean running;

    @Override
    public void start(int port, int threads) {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        running = true;
        final InetSocketAddress addr = new InetSocketAddress(port);
        for (int i = 1; i <= threads; i++) {
            pool.execute(() -> {
                byte[] recieveData = new byte[1024];
                try (DatagramSocket socket = new DatagramSocket(port)) {
                    socket.setSoTimeout(100);
                    while (running) {
                        try {
                            DatagramPacket receivePacket = new DatagramPacket(recieveData, 0, recieveData.length);
                            socket.receive(receivePacket);
                            String message = "Hello, " + new String(receivePacket.getData(), 0, receivePacket.getLength());
                            System.out.println(message);
                            //message = message.concat(new String(receivePacket.getData(), 0, receivePacket.getLength()));
                            DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), 0, message.length(), addr);
                            System.out.println(sendPacket.toString());
                            socket.send(sendPacket);
                            System.out.println("sending");
                        } catch (IOException e) {
                            System.out.println("Sorry, we have problems with data(");
                        }
                    }
                } catch (SocketException e) {
                    System.out.println("Sorry, we have some problems with socket(");
                }
            });
        }
    }

    @Override
    public void close() {
        running = false;
    }
}
