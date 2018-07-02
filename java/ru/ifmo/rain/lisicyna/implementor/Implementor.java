package ru.ifmo.rain.lisicyna.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;
import org.jsoup.select.Evaluator;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class Implementor implements JarImpler {

    class Cleaner extends SimpleFileVisitor<Path> {
        Cleaner() {
            super();
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.deleteIfExists(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.deleteIfExists(dir);
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * Create correct full description of of all methods of the class
     * @see java.lang.reflect.Method
     * @param token type token to create methods
     * @return String with correct methods
     */
    private String writeMethods(Class<?> token) {
        StringBuilder result = new StringBuilder("");
        for (Method m : token.getMethods()) {
            int modifiers = m.getModifiers();
            Class returnType = m.getReturnType();
            if (!Modifier.isPrivate(modifiers)) {
                if (Modifier.isStatic(modifiers)) {
                    result.append("static ");
                }
                if (Modifier.isFinal(modifiers)) {
                    result.append("final ");
                }
                if (Modifier.isPublic(modifiers)) {
                    result.append("public ");
                }
                if (Modifier.isProtected(modifiers)){
                    result.append("protected ");
                }
                result.append(returnType.getCanonicalName() + " " + m.getName() + "(");
                Class t[] = m.getParameterTypes();
                for (int i = 0; i < t.length - 1; i++) {
                    result.append(t[i].getCanonicalName() + " a" + i + ", ");
                }
                if (t.length > 0) {
                    result.append(t[t.length - 1].getCanonicalName() + " a" + (t.length - 1) + ") {\n");
                } else {
                    result.append(") {\n");
                }
                if (returnType.isPrimitive()) {
                    if (returnType == void.class) {
                        result.append("return;\n");
                    } else if (returnType == boolean.class) {
                        result.append("return false;\n");
                    } else {
                        result.append("return 0;\n");
                    }
                } else {
                    result.append("   return null;\n");
                }
                result.append("}\n");
            }
        }
        return new String(result);
    }

    /**
     * Implement <code>token</code>.
     * Create java file with implementation of <code>token</code>.
     * @see java.lang.Class
     * @param token type token to create implementation for.
     * @param root root directory.
     * @throws ImplerException if <code>token</code> can't be implemented
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (!token.isInterface()) {
            throw new ImplerException("Sorry, class - " + token.getSimpleName() + " is not interface");
        }
        File outputFile = new File(root.toFile(), token.getCanonicalName().replace(".", File.separator) + "Impl.java");
        //String fileDir = root.toFile().getAbsolutePath() + File.separator + token.getPackage().getName().replace(".", File.separator) + File.separator;
        outputFile.getParentFile().mkdirs();
        try (BufferedWriter output = Files.newBufferedWriter(outputFile.toPath(), Charset.forName("UTF-8"))) {
            if (token.getPackage() != null) {
                output.write("package " + token.getPackage().getName() + ";\n");
            }
            output.write("public class " + token.getSimpleName() + "Impl implements " + token.getSimpleName() + "{");
            output.newLine();
            output.write(writeMethods(token));
            output.write("}\n");
        } catch (IOException | InvalidPathException e) {

        }
    }

    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (!token.isInterface()) {
            throw new ImplerException("Sorry this token is not interface");
        }
        if (jarFile == null) {
            throw new ImplerException("Sorry this jarfile name is null");
        }
        Path workingDirectory;
        try {
            workingDirectory = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "tempImpl");
        } catch (IOException e) {
            throw new ImplerException("Sorry, can't create file");
        }
        String path;
        if (token.getPackage() != null) {
            path = token.getPackage().getName().replace(".", File.separator) + File.separator;
        } else {
            path = File.separator;
        }
        implement(token, workingDirectory);
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<String> args = new ArrayList<String>();
        args.add(workingDirectory.toAbsolutePath() +  File.separator + path + token.getSimpleName() + "Impl.java");
        args.add("-cp");
        args.add(workingDirectory + File.pathSeparator + System.getProperty("java.class.path"));
        compiler.run(null, null, null, args.toArray(new String[args.size()]));
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (OutputStream fileStream = Files.newOutputStream(jarFile.toAbsolutePath())) {
            try (JarOutputStream jarStream = new JarOutputStream(fileStream, manifest)) {
                InputStream inputStream = Files.newInputStream(Paths.get(workingDirectory.toAbsolutePath() + File.separator + path + token.getSimpleName() + "Impl.class"));
                jarStream.putNextEntry(new ZipEntry(path + token.getSimpleName() + "Impl.class"));
                byte[] buffer = new byte[1024];
                int c;
                while ((c = inputStream.read(buffer)) > 0) {
                    jarStream.write(buffer, 0, c);
                }
                jarStream.closeEntry();
            } catch (IOException e) {
                throw new ImplerException("Sorry, problem with creating JarOutputStream");
            }
        } catch (IOException | InvalidPathException e) {
            throw new ImplerException("Sorry, problem with file's working");
        } finally {
            try {
                Files.walkFileTree(workingDirectory, new Cleaner());
            } catch (IOException e){
                throw new ImplerException("Sorry, we have some problem with cleaning temp directory");
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Sorry, wrong number of arguments. Need 2 arguments");
            return;
        }
        try {
            String className = args[0];
            String fileName = args[1];
            Class token = Class.forName(className);
            Implementor implementor = new Implementor();
            if (fileName.substring(fileName.length() - 4).equals(".jar")) {
                implementor.implementJar(token, Paths.get(fileName));
            }else {
                implementor.implement(token, Paths.get(fileName));
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Sorry, couldn't get class from " + args[0]);
        } catch (ImplerException e) {
            System.out.println("Sorry, couldn't implement " + args[0]);
        }
    }
}
