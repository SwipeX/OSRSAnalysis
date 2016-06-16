package pw.tdekk.deob;


import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import pw.tdekk.Application;

/**
 * Created by TimD on 6/16/2016.
 */
public class UnusedVisitor extends MethodVisitor {
    public UnusedVisitor() {
        super(Opcodes.ASM5);
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
                               final String name, final String desc) {
        ClassNode parent = Application.archive.classes().get(owner);
        if (parent != null) {
            FieldNode field = parent.getField(name, desc);
            if (field != null) {
                field.referenceCount++;
            }
        }
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner,
                                final String name, final String desc) {
        ClassNode parent = Application.archive.classes().get(owner);
        if (parent != null) {
            MethodNode method = parent.getMethod(name, desc);
            if (method != null) {
                method.referenceCount++;
            }
        }
    }
}
