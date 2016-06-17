package pw.tdekk.deob;

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
import java.util.stream.Stream;

/**
 * Created by TimD on 6/17/2016.
 */
public class CallGraph extends DirectedGraph<Handle, Handle> {

    private ArrayList<Handle> calledMethods = new ArrayList<>();
    private ArrayList<Handle> calledFields = new ArrayList<>();

    private List<MethodNode> entryMethods() {
        List<MethodNode> entryPoints = new ArrayList<>();
        Application.archive.classes().values().forEach(c -> entryPoints.addAll(c.methods.stream().filter(m -> m.name.length() > 2).collect(Collectors.toList())));
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

    public ArrayList<Handle> getCalledFields() {
        return calledFields;
    }

    private void build(MethodNode method) {
        if (!calledMethods.contains(method.getHandle())) {
            method.accept(new MethodVisitor(Opcodes.ASM5) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                    ClassNode node = Application.archive.classes().get(owner);
                    if (node != null) {
                        MethodNode called = node.getMethod(name, desc);
                        if (called != null) {
                            calledMethods.add(method.getHandle());
                            called.referenceCount++;
                            insertEdge(method.getHandle(), called.getHandle());
                            build(called);
                        }
                    }
                }


                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                    ClassNode node = Application.archive.classes().get(owner);
                    if (node != null) {
                        FieldNode called = node.getField(name, desc);
                        if (called != null) {
                            called.referenceCount++;
                            if (!calledFields.contains(called.getHandle()))
                                calledFields.add(called.getHandle());
                        }
                    }
                }

            });
        }
    }


}
