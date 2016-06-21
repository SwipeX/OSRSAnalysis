package pw.tdekk.deob;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import pw.tdekk.Application;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by TimD on 6/21/2016.
 */
public class UnusedMethods implements Mutator {
    private ArrayList<Handle> usedMethods = new ArrayList<>();
    private int removedCount = 0;

    @Override
    public void mutate() {
        long startTime = System.currentTimeMillis();
        getEntryPoints().forEach(this::visit);
        ArrayList<MethodNode> toRemove = new ArrayList<>();
        Application.getClasses().values().forEach(c -> c.methods.forEach(m -> {
            if (!usedMethods.contains(m.getHandle())) {
                toRemove.add(m);
            }
        }));
        toRemove.forEach(m -> {
            m.owner.methods.remove(m);
            removedCount++;
        });
        System.out.println(String.format("Removed %s methods in %s ms", removedCount, (System.currentTimeMillis() - startTime)));
    }

    private void visit(MethodNode mn) {
        Handle handle = mn.getHandle();
        if (usedMethods.contains(handle)) return;
        usedMethods.add(handle);
        //subclass methods
        if ((mn.access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT) {
            Application.getClasses().values().stream().filter(node -> node.superName.equals(mn.owner.name)).forEach(node -> {
                MethodNode sub = node.getMethod(mn.name, mn.desc);
                if (sub != null)
                    visit(sub);
            });
        }
        mn.accept(new MethodVisitor(Opcodes.ASM5) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (Application.getClasses().containsKey(owner)) {
                    ClassNode node = Application.getClasses().get(owner);
                    MethodNode method = node.getMethod(name, desc);
                    if (method != null) {
                        visit(method);
                        String superName = node.superName;
                        if (Application.getClasses().containsKey(superName)) {
                            ClassNode superClass = Application.getClasses().get(superName);
                            MethodNode superMethod = superClass.getMethod(name, desc);
                            if (superMethod != null) {
                                visit(superMethod);
                            }
                        }
                    }
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        });
    }


    public List<MethodNode> getEntryPoints() {
        ArrayList<MethodNode> entry = new ArrayList<>();
        Application.getClasses().values().forEach(node -> {
            //All methods > 2 length for osrs obfuscator
            entry.addAll(node.methods.stream().filter(m -> m.name.length() > 2).collect(Collectors.toList()));
            //Any subclass methods should be added
            String superName = node.superName;
            if (Application.getClasses().containsKey(superName)) {
                for (MethodNode method : node.methods) {
                    entry.addAll(Application.getClasses().get(superName).methods.stream().filter(m ->
                            m.name.equals(method.name) && m.desc.equals(method.desc)).collect(Collectors.toList()));
                }
            }
            //interface methods
            List<String> interfaces = node.interfaces;
            for (String iface : interfaces) {
                ClassNode impl = Application.getClasses().get(iface);
                if (impl != null) {
                    for (MethodNode mn : node.methods) {
                        impl.methods.stream().filter(imn -> mn.name.equals(imn.name) && mn.desc.equals(imn.desc)).forEach(imn -> {
                            entry.add(mn);
                            entry.add(imn);
                        });
                    }
                }
            }
        });
        return entry;
    }
}
