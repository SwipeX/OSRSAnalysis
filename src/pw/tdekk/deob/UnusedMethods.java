package pw.tdekk.deob;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import pw.tdekk.Application;
import pw.tdekk.deob.data.CallGraph;

import java.util.ArrayList;
import java.util.List;

import static pw.tdekk.Application.archive;

/**
 * Created by TimD on 6/20/2016.
 */
public class UnusedMethods implements AbstractTransform {
    private int fremoved, mremoved;
    private List<Handle> calledFields;

    @Override
    public void transform() {
        CallGraph callGraph = new CallGraph();
        callGraph.build();
        for (ClassNode classNode : archive.classes().values()) {
            Application.methodCount += classNode.methods.size();
            Application.fieldCount += classNode.fields.size();
            classNode.accept(new ClassVisitor(Opcodes.ASM5) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    archive.classes().values().stream().filter(c -> c.superName.equals(classNode.name)).forEach(cn -> {
                        MethodNode node = cn.getMethod(name, desc);
                        if (node != null) {
                            callGraph.getCalledMethods().add(node.getHandle());
                        }
                    });
                    if ((access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT) {
                        callGraph.getCalledMethods().add(new Handle(0, classNode.name, name, desc));
                    }
                    MethodNode node = classNode.getMethod(name, desc);
                    if(node.isOverride)
                        callGraph.getCalledMethods().add(node.getHandle());
                    return super.visitMethod(access, name, desc, signature, exceptions);
                }
            });

        }


        calledFields = new ArrayList<>();
        for (Handle handle : callGraph.getCalledMethods()) {
            MethodNode mn = archive.classes().get(handle.getOwner()).getMethod(handle.getName(), handle.getDesc());
            mn.accept(new MethodVisitor(Opcodes.ASM5) {
                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                    ClassNode node = archive.classes().get(owner);
                    if (node != null) {
                        FieldNode fn = node.getField(name, desc);
                        if (fn != null) {
                            calledFields.add(fn.getHandle());
                        }
                    }
                    super.visitFieldInsn(opcode, owner, name, desc);
                }
            });
        }
        archive.classes().values().forEach(cn -> {
            cn.methods.forEach(mn -> {
                if (!callGraph.getCalledMethods().contains(mn.getHandle())) {
                    cn.methods.remove(mn);
                    mremoved++;
                }
            });
            cn.fields.forEach(fn -> {
                if (!calledFields.contains(fn.getHandle())) {
                    cn.fields.remove(fn);
                    fremoved++;
                }
            });
        });
        System.out.println("Removed " + mremoved + " unused methods");
        System.out.println("Removed " + fremoved + " unused fields");
    }
}
