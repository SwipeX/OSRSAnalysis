package pw.tdekk;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Christopher Carpenter
 */
abstract class Archive {
    boolean built;

    /**
     * Completely reads an open input stream and then closes it.
     *
     * @param in The InputStream to be read
     * @return a non-null {@code byte[]} containing the results the now closed {@code InputStream}
     * @throws IOException when an issue prevents proper stream reading
     */
    static byte[] readInputStream(InputStream in) throws IOException {
        try (ReadableByteChannel inChannel = Channels.newChannel(in)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (WritableByteChannel outChannel = Channels.newChannel(baos)) {
                ByteBuffer buffer = ByteBuffer.allocate(4096);
                while (inChannel.read(buffer) != -1) {
                    buffer.flip();
                    outChannel.write(buffer);
                    buffer.compact();
                }
                buffer.flip();
                while (buffer.hasRemaining()) {
                    outChannel.write(buffer);
                }
                return baos.toByteArray();
            }
        }
    }

    /**
     * Gets the build state, that is whether or not this Archive has been built.
     *
     * @return true if this Archive has been built.
     */
    boolean built() {
        return built;
    }

    protected abstract ConcurrentMap<String, ClassNode> classes();

    protected abstract ConcurrentMap<String, byte[]> resources();

    public abstract long build() throws IOException;

    /**
     * Dispatches the given visitor to all the loaded classes.
     *
     * @param cfv The visitor to dispatch.
     */
    public void dispatch(ClassVisitor cfv) {
        if (!built()) {
            throw new IllegalStateException("The JarArchive must be built before visitors can be dispatched.");
        }
        for (ClassNode factory : classes().values()) {
            factory.accept(cfv);
        }
    }

    /**
     * Dispatches the given visitor to all the loaded methods.
     *
     * @param mv The visitor to dispatch.
     */
    public void dispatch(MethodVisitor mv) {
        if (!built()) {
            throw new IllegalStateException("The JarArchive must be built before visitors can be dispatched.");
        }
        for (ClassNode factory : classes().values()) {
            for (MethodNode mn : factory.methods)
                mn.accept(mv);
        }
    }

    public abstract void write() throws IOException;

    /**
     * Clears the contents of the classes and resources and resets {@code built} to false.
     */
    public void reset() {
        classes().clear();
        resources().clear();
        built = false;
    }
}