package pw.tdekk.mod.hooks;

import pw.tdekk.mod.Crypto;
import org.objectweb.asm.tree.MethodNode;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Tyler Sedlar
 */
public class InvokeHook extends Hook {

    public String clazz, method, desc, returnDesc;
    public int predicate = Integer.MAX_VALUE;
    public boolean isStatic;
    public Class<?> predicateType = int.class;

    public InvokeHook(String name, String clazz, String method, String desc, String returnDesc, boolean isStatic) {
        super(name);
        this.clazz = clazz;
        this.method = method;
        this.desc = desc;
        this.returnDesc = returnDesc;
        this.isStatic = isStatic;
    }

    public InvokeHook(String name, String clazz, String method, String desc, boolean isStatic) {
        this(name, clazz, method, desc, desc, isStatic);
    }

    public InvokeHook(String name, String clazz, String method, String desc) {
        this(name, clazz, method, desc, desc, false);
    }

    public InvokeHook(String name, MethodNode mn, String returnDesc) {
        this(name, mn.owner.name, mn.name, mn.desc, returnDesc, (mn.access & ACC_STATIC) > 0);
    }

    public InvokeHook(String name, MethodNode mn) {
        this(name, mn, mn.desc);
    }

    public void setOpaquePredicate(int predicate, Class<?> predicateType) {
        this.predicate = predicate;
        this.predicateType = predicateType;
    }

    @Override
    public byte getType() {
        return Type.INVOKE;
    }

    @Override
    public String getOutput() {
        String out = "& " + name + " --> " + clazz + "." + method + desc + " (" + returnDesc + ")";
        if (predicate != Integer.MAX_VALUE) {
            out += " [" + predicate + "] - " + predicateType;
        }
        return out;
    }

    @Override
    protected void writeData(DataOutputStream out) throws IOException {
        out.writeUTF(name);
        out.writeUTF(clazz);
        out.writeUTF(method);
        out.writeUTF(desc);
        out.writeUTF(returnDesc);
        out.writeBoolean(isStatic);
        out.writeInt(predicate);
        out.writeUTF(predicateType == int.class ? "I" : (predicateType == byte.class ? "B" : "S"));
    }


    @Override
    protected void writeEncryptedData(DataOutputStream out) throws IOException {
        out.writeUTF(Crypto.encrypt(name));
        out.writeUTF(Crypto.encrypt(clazz));
        out.writeUTF(Crypto.encrypt(method));
        out.writeUTF(Crypto.encrypt(desc));
        out.writeUTF(Crypto.encrypt(returnDesc));
        out.writeBoolean(isStatic);
        out.writeInt(predicate);
        out.writeUTF(Crypto.encrypt(predicateType == int.class ? "I" : (predicateType == byte.class ? "B" : "S")));
    }
}