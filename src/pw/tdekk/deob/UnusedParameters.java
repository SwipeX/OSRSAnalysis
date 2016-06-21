package pw.tdekk.deob;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import pw.tdekk.Application;
import pw.tdekk.test.App;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TimD on 6/21/2016.
 */
public class UnusedParameters implements Mutator {
    private int totalRemoved = 0;

    @Override
    public void mutate() {
        for (ClassNode node : Application.getClasses().values()) {
            for (MethodNode mn : node.methods) {
                int offset = (mn.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
                Type[] types = Type.getArgumentTypes(mn.desc);
                int[] used = new int[types.length];
                mn.accept(new MethodVisitor(Opcodes.ASM5) {
                    @Override
                    public void visitVarInsn(int opcode, int var) {
                        int index = var - offset;
                        used[index]++;
                        super.visitVarInsn(opcode, var);
                    }
                });
                int removed = 0;
                for (int i = 0; i < types.length; i++) {
                    if (used[i] > 0) {
                        remove(mn, i - removed);
                        removed++;
                        used[i] = 0;
                    }
                }
                totalRemoved += removed;
            }
        }
    }

    private MethodNode remove(MethodNode mn, int paramIndex) {
        String newDesc = "(";
        Type[] types = Type.getArgumentTypes(mn.desc);
        for (int i = 0; i < types.length; i++) {
            if (i != paramIndex) {
                newDesc += types[i].getDescriptor();
            }
        }
        newDesc += ")" + Type.getReturnType(mn.desc).getDescriptor();
        System.out.println("changed " + mn.desc + " to " + newDesc);
        mn.desc = newDesc;
        return mn;
    }
}
