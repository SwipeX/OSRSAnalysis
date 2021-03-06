package pw.tdekk.visitor;

import pw.tdekk.Updater;
import pw.tdekk.mod.hooks.FieldHook;
import pw.tdekk.mod.hooks.Hook;
import pw.tdekk.mod.hooks.InvokeHook;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.graph.FlowGraph;
import org.objectweb.asm.commons.util.Assembly;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

public abstract class GraphVisitor implements Opcodes {

    public final Map<String, Hook> hooks = new HashMap<>();

    public Updater updater;
    public ClassNode cn = null;
    public FlowGraph graph;

    private String id = null;

    public abstract boolean validate(ClassNode cn);

    public abstract void visit();

    public final String id() {
        return id != null ? id : (id = getClass().getSimpleName());
    }

    public String iface() {
        return updater.getAccessorPrefix() + id();
    }

    public String clazz(String visitor) {
        try {
            return updater.visitor(visitor).cn.name;
        } catch (NullPointerException e) {
            return "null";
        }
    }

    public String desc() {
        return desc(id());
    }

    public String desc(String visitor) {
        return "L" + clazz(visitor) + ";";
    }

    public String literalDesc() {
        return desc(id());
    }

    public String literalDesc(String visitor) {
        return "L" + updater.visitor(visitor).id() + ";";
    }

    public String key(String hook) {
        FieldHook fh = (FieldHook) hooks.get(hook);
        return fh != null ? fh.clazz + "." + fh.field : null;
    }

    private String resolveDesc(String desc) {
        for (GraphVisitor visitor : updater.visitors) {
            if (visitor.cn == null) {
                continue;
            }
            desc = desc.replaceAll(visitor.desc(), "L" + visitor.iface() + ";");
        }
        return desc;
    }

    public final void addHook(Hook hook) {
        if (hook.name == null) {
            return;
        }
        if (hook instanceof FieldHook) {
            ((FieldHook) hook).returnDesc = resolveDesc(((FieldHook) hook).returnDesc);
        } else if (hook instanceof InvokeHook) {
            ((InvokeHook) hook).returnDesc = resolveDesc(((InvokeHook) hook).returnDesc);
        }
        hooks.put(hook.name, hook);
    }

    public final void add(String name, FieldNode fn) {
        if (name == null || fn == null) {
            return;
        }
        add(name, fn, resolveDesc(fn.desc));
    }

    public final void add(String name, FieldNode fn, String returnDesc) {
        if (name == null || fn == null) {
            return;
        }
        hooks.put(name, new FieldHook(name, fn, returnDesc));
    }

    public final void visit(String visitor, BlockVisitor bv) {
        ClassNode cn = updater.visitor(visitor).cn;
        if (cn == null) {
            return;
        }
        for (FlowGraph graph : updater.graphs().get(cn).values()) {
            this.graph = graph;
            for (Block block : graph) {
                if (bv.validate()) {
                    bv.visit(block);
                }
            }
        }
    }

    public final void visit(BlockVisitor bv) {
        visit(id(), bv);
        bv.visitEnd();
    }

    public final void visitAll(BlockVisitor bv) {
        for (Map<MethodNode, FlowGraph> map : updater.graphs().values()) {
            for (FlowGraph graph : map.values()) {
                this.graph = graph;
                for (Block block : graph) {
                    if (bv.validate()) {
                        bv.visit(block);
                    }
                }
            }
        }
        bv.visitEnd();
    }

    public final void visit(MethodVisitor mv) {
        for (MethodNode mn : cn.methods) {
            mn.accept(mv);
        }
    }

    public final void visitAll(MethodVisitor mv) {
        for (ClassNode cn : updater.classnodes.values()) {
            for (MethodNode mn : cn.methods) {
                mn.accept(mv);
            }
        }
    }

    public String getHookKey(String hook) {
        Hook h = hooks.get(hook);
        if (h == null) {
            return null;
        }
        FieldHook fh = (FieldHook) h;
        return fh.clazz + "." + fh.field;
    }

    public boolean methodHas(String hook, MethodNode mn) {
        return Assembly.first(mn.instructions, ain -> ain instanceof FieldInsnNode &&
                Assembly.keyFor((FieldInsnNode) ain).equals(getHookKey(hook))) != null;
    }

    public String reg(String regex) {
        return regex.replaceAll("\\[PRED\\]", "(?:(I|B|S))?");
    }
}