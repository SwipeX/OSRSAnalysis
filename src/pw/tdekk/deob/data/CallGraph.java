package pw.tdekk.deob.data;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import pw.tdekk.Application;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static pw.tdekk.Application.archive;

/**
 * Created by TimD on 6/17/2016.
 */
public class CallGraph extends DirectedGraph<Handle, Handle> {

    private ArrayList<Handle> calledMethods = new ArrayList<>();

    private List<MethodNode> entryMethods() {
        List<MethodNode> entryPoints = new ArrayList<>();
        Application.archive.classes().values().forEach(c -> {
            entryPoints.addAll(c.methods.stream().filter(m -> m.name.length() > 2).collect(Collectors.toList()));
            c.accept(new ClassVisitor(Opcodes.ASM5) {
                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    for (String iface : interfaces) {
                        if (archive.classes().containsKey(iface)) {
                            ClassNode node = archive.classes().get(iface);
                            for (MethodNode mn : node.methods) {
                                MethodNode located = c.getMethod(mn.name, mn.desc);
                                if (located != null) {
                                    entryPoints.add(located);
                                }
                            }
                        }
                    }
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    ClassNode node = Application.archive.classes().get(c.name);
                    if (node != null) {
                        MethodNode called = node.getMethod(name, desc);
                        if (called != null) {
                            ClassNode superClass = node.getSuperClass();
                            if (superClass != null) {
                                MethodNode superMethod = superClass.getMethod(name, desc);
                                if (superMethod != null) {
                                    entryPoints.add(superMethod);
                                }
                            }
                        }
                    }
                    return super.visitMethod(access, name, desc, signature, exceptions);
                }
            });

        });
        return entryPoints;
    }

    public void insertEdge(Handle initial, Handle destination) {
        addVertex(destination);
        addEdge(initial, destination);
    }

    public void build() {
        entryMethods().forEach(this::build);
    }

    public ArrayList<Handle> getCalledMethods() {
        return calledMethods;
    }


    private void build(MethodNode method) {
        if (!calledMethods.contains(method.getHandle())) {
            calledMethods.add(method.getHandle());
            method.accept(new MethodVisitor(Opcodes.ASM5) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                    ClassNode node = Application.archive.classes().get(owner);
                    if (node != null) {
                        MethodNode called = node.getMethod(name, desc);
                        if (called != null) {
                            archive.classes().values().stream().filter(c -> c.superName.equals(owner)).forEach(cn -> {
                                MethodNode sub = cn.getMethod(name, desc);
                                if (sub != null) {
                                    calledMethods.add(sub.getHandle());
                                    build(sub);
                                }
                            });
                            called.referenceCount++;
                            insertEdge(method.getHandle(), called.getHandle());
                            build(called);
                        }
                    }
                }
            });
        }
    }
}
