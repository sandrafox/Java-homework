package ru.ifmo.rain.lisicyna.walk;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

class FileVisitor extends SimpleFileVisitor<Path> {
    private Writer writer;
    private final static int FNV_PRIME = 0x01000193;

    public FileVisitor(Writer writer) {
        this.writer = writer;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        int hval = 0x811c9dc5;
        try (InputStream input = Files.newInputStream(file)) {
            byte[] b = new byte[1024];
            int c;
            while ((c = input.read(b)) >= 0) {
                hval = hash(b, c, hval);
            }
        } catch (IOException e) {
            hval = 0;
            System.out.println("Sorry, problem with reading from file " + file);
        } finally {
            write(hval, file.toString());
        }
        return FileVisitResult.CONTINUE;
    }

    private int hash(byte[] b, int c, int hval) {
        for (int i = 0; i < c; i++) {
            hval *= FNV_PRIME;
            hval ^= b[i] & 0xff;
        }
        return hval;
    }

    public void write(int value, String fileName) {
        try {
            writer.write(String.format("%08x", value) + " " + fileName + "\n");
        } catch (IOException e) {
            System.out.println("Sorry, cannot write to file");
        }
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        write(0, file.toString());
        System.out.println("Sorry, cannot visit file " + file);
        return FileVisitResult.CONTINUE;
    }
}