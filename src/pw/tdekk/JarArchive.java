package pw.tdekk;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * @author Tyler Sedlar
 * @author Christopher Carpenter
 * @version 1.1.0
 * @since 2/12/16
 */
public class JarArchive extends Archive {

    private final ConcurrentHashMap<String, ClassNode> classes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, byte[]> resources = new ConcurrentHashMap<>();
    private final File file;
    private Manifest manifest;

    /**
     * Constructs a {@code JarArchive} using the specified input file path.
     *
     * @param filePath the path to the {@code File} to build the JarArchive from.
     */
    public JarArchive(String filePath) {
        this(new File(filePath));
    }

    /**
     * Constructs a {@code JarArchive} using the specified input file.
     *
     * @param file the {@code File} to build the JarArchive from.
     */
    private JarArchive(File file) {
        this.file = file;
    }

    @Override
    public ConcurrentMap<String, ClassNode> classes() {
        if (!built()) {
            throw new IllegalStateException("The JarArchive must be built before its classes can be retrieved.");
        }
        return classes;
    }

    @Override
    public ConcurrentMap<String, byte[]> resources() {
        if (!built()) {
            throw new IllegalStateException("The JarArchive must be built before its resources can be retrieved.");
        }
        return resources;
    }

    /**
     * The jar archive file
     *
     * @return The jar archive file.
     */
    public File file() {
        return file;
    }


    /**
     * Builds the class and resource maps using a parallelismThreshold of 1 if {@code parallel}, otherwise Long.MAX_VALUE
     *
     * @return The amount of milliseconds it took to build the classes and resources
     * @throws IOException if an error occurs while locating, reading, or parsing the file
     */
    public long build() throws IOException {
        return build(true ? 1 : Long.MAX_VALUE);
    }

    /**
     * Builds the class and resource maps using the specified {@code parallelismThreshold}
     *
     * @param parallelismThreshold The parallelismThreshold to be used when parsing the file entries
     * @return The amount of milliseconds it took to build the classes and resources
     * @throws IOException if an error occurs while locating, reading, or parsing the file
     */
    private long build(long parallelismThreshold) throws IOException {
        long time = System.currentTimeMillis();
        if (built()) {
            throw new IllegalStateException("The JarArchive cannot be built more than once.");
        }
        try (JarFile jar = new JarFile(file)) {
            manifest = jar.getManifest();
            ArrayList<JarEntry> entries = Collections.list(jar.entries());
            ConcurrentHashMap<String, InputStream> entryStreams = new ConcurrentHashMap<>(entries.size());
            for (JarEntry entry : entries) {
                entryStreams.put(entry.getName(), jar.getInputStream(entry));
            }
            CopyOnWriteArrayList<IOException> forEachExceptions = new CopyOnWriteArrayList<>();
            entryStreams.forEach(parallelismThreshold, (name, input) -> {
                try {
                    if (name.endsWith(".class")) {
                        ClassNode cn = new ClassNode();
                        byte[] bytes = readInputStream(input);
                        ClassReader reader = new ClassReader(bytes);
                        reader.accept(cn, ClassReader.SKIP_FRAMES);
                        classes.put(name.replace(".class", ""), cn);
                    } else {
                       // resources.put(name, readInputStream(input));
                    }
                } catch (IOException ioe) {
                    forEachExceptions.add(ioe);
                } finally {
                    try {
                        input.close();
                    } catch (IOException ioe) {
                        forEachExceptions.add(ioe);
                    }
                }
            });
            if (!forEachExceptions.isEmpty()) {
                if (forEachExceptions.size() == 1) {
                    throw forEachExceptions.get(0);
                } else {
                    throw new IOException(
                            forEachExceptions.size() + " exceptions occurred while building the class and resource maps.");
                }
            }
        }
        built = true;
        return System.currentTimeMillis() - time;
    }

    /**
     * Writes the classes and resources to the specified file using the supplied ClassWriter flags.
     *
     * @param destinationFile The file to write to.
     * @param writerFlags     The ClassWriter flags to use.
     * @throws IOException If an error occurs while manipulating an output stream
     */
    void write(File destinationFile, int writerFlags) throws IOException {
        if (!built()) {
            throw new IllegalStateException("You cannot write a JarArchive until it has been built.");
        }
        try (JarOutputStream output = new JarOutputStream(new FileOutputStream(destinationFile))) {
            Map<String, String> hashes = new HashMap<>();
            for (Map.Entry<String, ClassNode> entry : classes.entrySet()) {
                ClassNode factory = entry.getValue();
                String entryKey = factory.name.replaceAll("\\.", "/") + ".class";
                output.putNextEntry(new JarEntry(entryKey));
                ClassWriter writer = new ClassWriter(0);
                factory.accept(writer);
                byte[] bytes = writer.toByteArray();
                output.write(bytes);
                output.closeEntry();
            }
            for (Map.Entry<String, byte[]> entry : resources.entrySet()) {
                String key = entry.getKey();
                byte[] bytes = entry.getValue();
                if (key.equals("META-INF/MANIFEST.MF")) {
                    manifest.getEntries().forEach((file, attrs) -> attrs.forEach((k, v) -> {
                        String lookupKey = k.toString();
                        if (lookupKey.equals("SHA1-Digest")) {
                            attrs.put(k, hashes.get(file));
                        }
                    }));
                    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        manifest.write(out);
                        bytes = out.toByteArray();
                    }
                }
                output.putNextEntry(new JarEntry(key));
                output.write(bytes);
                output.closeEntry();
            }
            output.flush();
        }
    }

    /**
     * Writes the classes and resources back to the original file using the specified ClassWriter flags.
     *
     * @param writerFlags The ClassWriter flags to use.
     * @throws IOException If an error occurs while manipulating an output stream
     */
    public void write(int writerFlags) throws IOException {
        write(file, writerFlags);
    }

    /**
     * Writes the classes and resources back to the original file using the ClassWriter flag COMPUTE_MAXS.
     *
     * @throws IOException If an error occurs while manipulating an output stream
     */
    @Override
    public void write() throws IOException {
        write(file, ClassWriter.COMPUTE_MAXS);
    }
}