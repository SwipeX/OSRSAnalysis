package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import pw.tdekk.mod.hooks.FieldHook;
import pw.tdekk.mod.hooks.InvokeHook;
import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.FieldMemberNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

@VisitorInfo(hooks = {"node", "queue", "table", "remaining", "size", "clear"})
public class Cache extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.getFieldTypeCount() == 4 && cn.fieldCount(desc("CacheableNode")) == 1 &&
                cn.fieldCount(desc("HashTable")) == 1;
    }

    @Override
    public void visit() {
        add("node", cn.getField(null, desc("CacheableNode")));
        add("queue", cn.getField(null, desc("Queue")));
        add("table", cn.getField(null, desc("HashTable")));
        visit(new ReadHooks());
        addClear();
    }

    private class ReadHooks extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTFIELD && fmn.owner().equals(cn.name) && fmn.desc().equals("I")) {
                        FieldMemberNode size = fmn.firstField();
                        if (size != null && size.opcode() == GETFIELD && size.owner().equals(cn.name) &&
                                size.desc().equals("I")) {
                            hooks.put("remaining", new FieldHook("remaining", fmn.fin()));
                            hooks.put("size", new FieldHook("size", size.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private void addClear() {
        for (MethodNode mn : cn.methods) {
            if ((mn.access & ACC_STATIC) > 0 || !mn.desc.equals("()V")) {
                continue;
            }
            addHook(new InvokeHook("clear", mn));
        }
    }
}
