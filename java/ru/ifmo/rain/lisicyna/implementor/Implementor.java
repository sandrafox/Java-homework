package ru.ifmo.rain.lisicyna.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Implementation of {@link JarImpler}
 * This class provides methods for generating implementation of class and creating jar file of that implementation
 *
 * @author Alexandra Lisicyna
 */
public class Implementor implements JarImpler {
    /**
     * Full path of generated source file
     */
    private Path filePath;
    /**
     * Cleaner for temp directories
     */
    private Cleaner cleaner = new Cleaner();

    /**
     * Extension of {@link SimpleFileVisitor}
     * This class provides method to clean directories
     */
    class Cleaner extends SimpleFileVisitor<Path> {

        /**
         * Default constructor
         * @see SimpleFileVisitor#SimpleFileVisitor()
         */
        Cleaner() {
            super();
        }

        /**
         * Delete file
         * For more details of work see {@link SimpleFileVisitor#visitFile(Object, BasicFileAttributes)} and {@link Files#delete(Path)}
         * @param file - name of file for deleting
         * @param attrs - not use in this method
         * @return {@link FileVisitResult#CONTINUE}
         * @throws IOException if cannot delete file
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        /**
         * Delete directory after deleting all files from this directory
         * @param dir - name of file for deleting
         * @param exc - not use in this method
         * @return {@link FileVisitResult#CONTINUE}
         * @throws IOException if cannot delete directory
         */
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * Create string representation of method
     * @see Method
     * @param m - method, that will be repsented
     * @return string representation of method
     */
    private StringBuilder writeMethod(Method m) {
        StringBuilder result = new StringBuilder("");
        result.append("\n    ");
        int modifiers = m.getModifiers();
        Class returnType = m.getReturnType();
            modifiers = modifiers & ~Modifier.ABSTRACT & ~Modifier.NATIVE & ~Modifier.TRANSIENT;
            result.append(Modifier.toString(modifiers));
            result.append(" " + returnType.getCanonicalName() + " " + m.getName() + "(");
            Class t[] = m.getParameterTypes();
            for (int i = 0; i < t.length - 1; i++) {
                result.append(t[i].getCanonicalName() + " a" + i + ", ");
            }
            if (t.length > 0) {
                result.append(t[t.length - 1].getCanonicalName() + " a" + (t.length - 1) + ")");
            } else {
                result.append(")");
            }
            Class<?>[] exceptions = m.getExceptionTypes();
            if (exceptions.length > 0) {
                result.append(" throws");
                for (Class<?> ex : exceptions) {
                    result.append(" " + ex.getCanonicalName());
                }
            }
            result.append(" {\n");
            if (returnType.isPrimitive()) {
                if (returnType == void.class) {
                    result.append("        return;\n");
                } else if (returnType == boolean.class) {
                    result.append("        return false;\n");
                } else {
                    result.append("        return 0;\n");
                }
            } else {
                result.append("       return null;\n");
            }
            result.append("    }\n");
        return result;
    }

    /**
     * Create correct full description of all abstract methods of the class
     * @see java.lang.reflect.Method
     * @see Modifier
     * @param token type token to create methods
     * @return String with correct methods
     */
    private String writeMethods(Class<?> token) {
        StringBuilder result = new StringBuilder("");
        Set<Integer> methods = new HashSet<>();
        for (Method m : token.getMethods()) {
            if (Modifier.isAbstract(m.getModifiers()) && methods.add(Objects.hash(m.getName(), Arrays.toString(m.getParameterTypes())))) {
                result.append(writeMethod(m));
            }
        }
        while (token != null) {
            for (Method m: token.getDeclaredMethods()) {
                if (Modifier.isAbstract(m.getModifiers()) && methods.add(Objects.hash(m.getName(), Arrays.toString(m.getParameterTypes())))) {
                    result.append(writeMethod(m));
                }
            }
            token = token.getSuperclass();
        }
        return new String(result);
    }

    /**
     * Create correct full description of all not-private constructors of the class
     * @see Constructor
     * @see Modifier
     * @param token - class, which consructors will be represented
     * @return string of description
     * @throws ImplerException when class does not have not-private constructors
     */
    private String writeConstructors(Class<?> token) throws ImplerException {
        StringBuilder result = new StringBuilder("");
        Constructor<?>[] constructors = Arrays.stream(token.getDeclaredConstructors()).filter(c -> !Modifier.isPrivate(c.getModifiers())).toArray(Constructor<?>[]::new);
        if (constructors.length == 0) {
            throw new ImplerException("Sorry, the token does not have not-private constructors");
        }
        for (Constructor c : constructors) {
            result.append("\n    ");
            int modifiers = c.getModifiers();
            if (!Modifier.isPrivate(modifiers)) {
                modifiers = modifiers & ~Modifier.ABSTRACT & ~Modifier.NATIVE & ~Modifier.TRANSIENT;
                result.append(Modifier.toString(modifiers));
                result.append(" " + c.getDeclaringClass().getSimpleName() + "Impl (");

                StringBuilder params = new StringBuilder();
                Class t[] = c.getParameterTypes();
                for (int i = 0; i < t.length - 1; i++) {
                    result.append(t[i].getCanonicalName() + " a" + i + ", ");
                    params.append(" a" + i + ", ");
                }
                if (t.length > 0) {
                    result.append(t[t.length - 1].getCanonicalName() + " a" + (t.length - 1) + ")");
                    params.append(" a" + (t.length - 1) + ")");
                } else {
                    params.append(")");
                    result.append(")");
                }
                Class<?>[] exceptions = c.getExceptionTypes();
                if (exceptions.length > 0) {
                    result.append(" throws");
                    for (Class<?> ex : exceptions) {
                        result.append(" " + ex.getCanonicalName());
                    }
                }
                result.append(" {\n");
                result.append("        super(").append(params).append(";\n    }\n");
            }
        }
        return new String(result);
    }

    /**
     * Implement or extend <code>token</code>.
     * Create java file with implementation or extension of <code>token</code>.
     * @see java.lang.Class
     * @param token type token to create implementation for.
     * @param root root directory.
     * @throws ImplerException if <code>token</code> can't be implemented
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token.isPrimitive()) {
            throw new ImplerException("Sorry, the token is a primitive");
        }
        if (token.isArray()) {
            throw new ImplerException("Sorry, the token is an array");
        }
        if (token.equals(Enum.class)) {
            throw new ImplerException("Sorry, the token is an enum");
        }
        if (Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Sorry, the token is a final class");
        }
        filePath = root.resolve(token.getPackage().getName().replace(".", File.separator)).resolve(token.getSimpleName() + "Impl.java");
        if (filePath.getParent() != null) {
            try {
                Files.createDirectories(filePath.getParent());
            } catch (IOException e) {
                throw new ImplerException("Sorry, can't create directory");
            }
        }
        try (BufferedWriter output = Files.newBufferedWriter(filePath, Charset.forName("UTF-8"))) {
            if (token.getPackage() != null) {
                output.write("package " + token.getPackage().getName() + ";\n\n");
            }
            output.write("public class " + token.getSimpleName() + "Impl " + (token.isInterface() ? "implements " : "extends ") + token.getSimpleName() + "{");
            output.newLine();
            if (!token.isInterface()) {
                output.write(writeConstructors(token));
            }
            output.write(writeMethods(token));
            output.write("}\n");
        } catch (IOException | InvalidPathException e) {

        }
    }

    /**
     * Create .jar file from implementation or extension of <code>token</code>
     * Use method {@link #implement(Class, Path)} for creating implementation or extension. After that compile file creating .jar.
     * @see JavaCompiler
     * @see JarOutputStream
     * @param token type token to create implementation for.
     * @param jarFile target <tt>.jar</tt> file.
     * @throws ImplerException if cannot implement <code>token</code> or cannot create .jar file
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "temp");
        } catch (IOException e) {
            throw new ImplerException("Sorry, cannot create temp directory", e);
        }
        try {
            implement(token, tempDir);
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            String[] args = new String[]{
                    "-cp",
                    tempDir.toString() + File.pathSeparator + System.getProperty("java.class.path"),
                    filePath.toString()
            };
            if (compiler == null || compiler.run(null, null, null, args) != 0) {
                throw new ImplerException("Unable to compile generated files");
            }
            if (jarFile.getParent() != null) {
                try {
                    Files.createDirectories(jarFile.getParent());
                } catch (IOException e) {
                    throw new ImplerException("Sorry, cannot create directories for jar file", e);
                }
            }
            Manifest manifest = new Manifest();
            Attributes attributes = manifest.getMainAttributes();
            attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            try (JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
                writer.putNextEntry(new ZipEntry(token.getName().replace('.', '/') + "Impl.class"));
                Files.copy(tempDir.resolve(token.getPackage().getName().replace(".", File.separator)).resolve(token.getSimpleName() + "Impl.class"), writer);
            } catch (IOException e) {
                throw new ImplerException("Unable to write to JAR file", e);
            }
        } finally {
            try {
                Files.walkFileTree(tempDir, cleaner);
            } catch (IOException e) {
                throw new ImplerException("Unable to delete temp directory: ", e);
            }
        }
    }

    /**
     * Provides ability to run Implementor using command line
     * All of errors will be written on console
     * Usage:
     * <ul>
     *     <li>Implementor class name path</li>
     *     <li>Implementor -jar class name path to jar file</li>
     * </ul>
     * @param args - the command line arguments
     */
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Sorry, wrong number of arguments.\n" +
                    "Usage: Implementor <class name> <path>\n" +
                    "or Implementor -jar <class name> <path to jar file");
            return;
        }
        Implementor implementor = new Implementor();
        try {
        if (args.length == 3) {
            if (args[0] == null || args[1] == null || args[2] == null) {
                System.out.println("Sorry, some of your arguments are null");
                return;
            }
            if (!args[0].equals("-jar")) {
                System.out.println("Sorry, but if you use 3 arguments usage: Implementor -jar <class name> <path to jar file>");
                return;
            }
            implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
        } else {
            if (args[0] == null || args[1] == null) {
                System.out.println("Sorry, some of your arguments are null");
                return;
            }
            implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
        }
        } catch (ClassNotFoundException e) {
            System.out.println("Sorry, but the " + args[args.length %2] + " is invalid class name");
        } catch (ImplerException e) {
            System.out.println("Sorry, we have some problem with implementing class " + args[args.length % 2] + ": " + e.getMessage());
        }
    }
}
