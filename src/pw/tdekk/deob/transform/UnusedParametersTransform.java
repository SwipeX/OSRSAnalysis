package pw.tdekk.deob.transform;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by TimD on 6/21/2016.
 */
public class UnusedParametersTransform extends Transform {
    private int totalRemoved = 0;

    private MethodNode remove(MethodNode mn, List<Integer> targets) {
//        final int[] call = {0};
//        for (ClassNode node : Application.getClasses().values()) {
//            for (MethodNode method : node.methods) {
//                method.accept(new MethodVisitor(Opcodes.ASM5) {
//                    @Override
//                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
//                        if (owner.equals(mn.owner.name) && name.equals(mn.name) && desc.equals(mn.desc)) {
//                            call[0]++;
//                            //alter param loads to not have @targets
//                        }
//                        super.visitMethodInsn(opcode, owner, name, desc, itf);
//                    }
//                });
//
//            }
//        }
//        System.out.println("Altered "+ call[0] +" calls");
        String newDesc = "(";
        Type[] types = Type.getArgumentTypes(mn.desc);
        for (int i = 0; i < types.length; i++) {
            if (!targets.contains(i)) {
                newDesc += types[i].getDescriptor();
            }
        }
        newDesc += ")" + Type.getReturnType(mn.desc).getDescriptor();
        //    System.out.println("changed " + mn.desc + " to " + newDesc);
        mn.desc = newDesc;
        return mn;
    }

    @Override
    public void transform(Map<String, ClassNode> classes) {
        long startTime = System.currentTimeMillis();
        for (ClassNode node : classes.values()) {
            for (MethodNode mn : node.methods) {
                int offset = (mn.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
                Type[] types = Type.getArgumentTypes(mn.desc);
                List<Integer> used = new ArrayList<>();
                mn.accept(new MethodVisitor() {
                    @Override
                    public void visitVarInsn(VarInsnNode vin) {
                        int index = vin.var - offset;
                        used.add(index);
                    }
                });
                if (used.size() == 0) continue;
                List<Integer> targets = new ArrayList<>();
                for (int i = 0; i < types.length; i++) {
                    if (!used.contains(i))
                        targets.add(i);
                }
                if (targets.size() == 0) continue;
                Collections.sort(targets, Collections.reverseOrder());
                remove(mn, targets);//TODO make this actually remove them
                totalRemoved += targets.size();
            }
        }
        System.out.println("Removed " + totalRemoved + " unused parameters in " + (System.currentTimeMillis() - startTime) + " ms");
    }
}
