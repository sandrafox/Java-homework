package ru.ifmo.rain.lisicyna.walk;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.ServerError;

public class Walk {
    static public void main(String[] args) {
        if (args.length != 2) return;
        try (BufferedReader input = Files.newBufferedReader(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            try (BufferedWriter output = Files.newBufferedWriter(Paths.get(args[1]),
                    StandardCharsets.UTF_8)) {
                FileVisitor visitor = new FileVisitor(output);
                String line;
                while ((line = input.readLine()) != null) {
                    try {
                        Files.walkFileTree(Paths.get(line), visitor);
                    } catch (InvalidPathException e) {
                        visitor.write(0, line);
                        System.out.println("Sorry, this is not a path " + line);
                    } catch (SecurityException e) {
                        visitor.write(0, line);
                        System.out.println("Sorry, cannot access to file " + line);
                    }
                }
            } catch (IOException | InvalidPathException e) {
                return;
            }
        } catch (IOException | InvalidPathException e) {
            return;
        }
    }
}
