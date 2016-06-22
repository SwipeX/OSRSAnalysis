package pw.tdekk.mod.hooks;

import pw.tdekk.mod.Crypto;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * @author Tim Dekker, Tyler Sedlar
 * @since 4/2/14
 */
public class CodeHook extends Hook {

    public String name, className, methodName, methodDesc;
    public int injectLocation;
    public InsnList instructions;
    public boolean inject = false;

    public CodeHook(String name, String className, String methodName, String methodDesc,
                    int injectLocation, InsnList instructions) {
        super(name);
        this.name = name;
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.injectLocation = injectLocation;
        this.instructions = instructions;
    }

    public CodeHook(String name, MethodNode mn, int injectLocation, InsnList instructions) {
        this(name, mn.owner.name, mn.name, mn.desc, injectLocation, instructions);
    }
    public CodeHook(String className, String name, MethodNode mn, int injectLocation, InsnList instructions) {
        this(name, className, mn.name, mn.desc, injectLocation, instructions);
    }
    @Override
    public byte getType() {
        return Type.CODE;
    }

    @Override
    public String getOutput() {
        StringBuilder output = new StringBuilder();
        output.append("+ ").append(name).append(" --> ");
        output.append(className).append(".").append(methodName).append(methodDesc).append(" @ ").append(injectLocation);
        return output.toString();
    }

    public String toString() {
        return getOutput();
    }

    @Override
    protected void writeData(DataOutputStream out) throws IOException {
        out.writeUTF(name);
        out.writeUTF(className);
        out.writeUTF(methodName);
        out.writeUTF(methodDesc);
        out.writeInt(injectLocation);
        out.writeBoolean(inject);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(instructions);
            }
            byte[] bytes = baos.toByteArray();
            out.writeInt(bytes.length);
            out.write(bytes, 0, bytes.length);
        }
    }

    @Override
    protected void writeEncryptedData(DataOutputStream out) throws IOException {
        out.writeUTF(Crypto.encrypt(name));
        out.writeUTF(Crypto.encrypt(className));
        out.writeUTF(Crypto.encrypt(methodName));
        out.writeUTF(Crypto.encrypt(methodDesc));
        out.writeInt(injectLocation);
        out.writeBoolean(inject);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(instructions);
            }
            byte[] bytes = baos.toByteArray();
            out.writeInt(bytes.length);
            out.write(bytes, 0, bytes.length);
        }
    }
}