package ru.ifmo.rain.lisicyna.walk;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

public class Walk {
    private final static int FNV_PRIME = 0x01000193;
    private final static BigInteger mod = new BigInteger("2").pow(32);

    private static String fnv(String name) throws IOException {
        String result;
        try (InputStream input = Files.newInputStream(Paths.get(name))) {
            byte[] b = new byte[1024];
            int c;
            int hval = 0x811c9dc5;
            while ((c = input.read(b)) >= 0) {
                for (int i = 0; i < c; ++i) {
                    hval *= FNV_PRIME;
                    hval ^= b[i] & 0xff;
                }
            }
            result = String.format("%08x", hval);
        } catch (IOException | InvalidPathException e) {
            result = "00000000";
        }
        return result;
    }

    static public void main(String[] args) {
        if (args.length != 2) return;
        try (BufferedReader input = Files.newBufferedReader(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            try (BufferedWriter output = Files.newBufferedWriter(Paths.get(args[1]),
                    StandardCharsets.UTF_8)) {
                String line;
                while ((line = input.readLine()) != null) {
                    output.write(fnv(line) + ' ' + line);
                    output.newLine();
                }
            } catch (IOException | InvalidPathException e) {
                return;
            }
        } catch (IOException | InvalidPathException e) {
            return;
        }
    }
}
