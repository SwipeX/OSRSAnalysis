package pw.tdekk.mod.hooks;

import pw.tdekk.mod.Crypto;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Tyler Sedlar
 */
public class FieldHook extends Hook {

    public String clazz;
    public String field;
    public String fieldDesc, returnDesc;
    public boolean isStatic, requiresSetter;

    public int multiplier = -1;

    public FieldHook(String name, String clazz, String field, String fieldDesc, String returnDesc, boolean isStatic, boolean requiresSetter) {
        super(name);
        this.clazz = clazz;
        this.field = field;
        this.fieldDesc = fieldDesc;
        this.returnDesc = returnDesc;
        this.isStatic = isStatic;
        this.requiresSetter = requiresSetter;
    }

    public FieldHook(String name, String clazz, String method, String desc, boolean isStatic) {
        this(name, clazz, method, desc, desc, isStatic, false);
    }

    public FieldHook(String name, String clazz, String method, String desc) {
        this(name, clazz, method, desc, false);
    }

    public FieldHook(String name, String clazz, String field, String fieldDesc, String returnDesc) {
        this(name, clazz, field, fieldDesc, returnDesc, false, false);
    }

    public FieldHook(String name, FieldInsnNode fin, String returnDesc) {
        this(name, fin.owner, fin.name, fin.desc, returnDesc, fin.opcode() == GETSTATIC || fin.opcode() == PUTSTATIC, false);
    }

    public FieldHook(String name, FieldInsnNode fin) {
        this(name, fin, fin.desc);
    }

    public FieldHook(String name, FieldNode fn, String returnDesc) {
        this(name, fn.owner.name, fn.name, fn.desc, returnDesc, (fn.access & ACC_STATIC) > 0, false);
    }

    public FieldHook(String name, FieldNode fn) {
        this(name, fn, fn.desc);
    }

    @Override
    public byte getType() {
        return Hook.Type.FIELD;
    }

    @Override
    public String getOutput() {
        StringBuilder output = new StringBuilder();
        output.append("# ").append(name).append(" --> ").append(clazz).append('.').append(field);
        if (multiplier != -1) {
            output.append(" * ").append(multiplier);
        }
        output.append(" - ");
        if (isStatic) {
            output.append("static ");
        }
        output.append(fieldDesc);
        output.append(" - ");
        output.append(returnDesc);
        return output.toString();
    }

    @Override
    protected void writeData(DataOutputStream out) throws IOException {
        out.writeUTF(name);
        out.writeUTF(clazz);
        out.writeUTF(field);
        out.writeUTF(fieldDesc);
        out.writeUTF(returnDesc);
        out.writeBoolean(isStatic);
        out.writeBoolean(requiresSetter);
        out.writeInt(multiplier);
    }

    @Override
    protected void writeEncryptedData(DataOutputStream out) throws IOException {
        out.writeUTF(Crypto.encrypt(name));
        out.writeUTF(Crypto.encrypt(clazz));
        out.writeUTF(Crypto.encrypt(field));
        out.writeUTF(Crypto.encrypt(fieldDesc));
        out.writeUTF(Crypto.encrypt(returnDesc));
        out.writeBoolean(isStatic);
        out.writeBoolean(requiresSetter);
        out.writeInt(multiplier);
    }
}
