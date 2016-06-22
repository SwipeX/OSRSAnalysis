package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import pw.tdekk.mod.hooks.FieldHook;
import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.AbstractNode;
import org.objectweb.asm.commons.cfg.tree.node.FieldMemberNode;
import org.objectweb.asm.tree.ClassNode;

@VisitorInfo(hooks = {"female", "appearance"})
public class PlayerDefinition extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.ownerless() && cn.getFieldTypeCount() == 4 && cn.fieldCount("J") == 2;
    }

    @Override
    public void visit() {
        add("female", cn.getField(null, "Z"), "Z");
        visit(new Appearance());
    }

    private class Appearance extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visit(AbstractNode n) {
                    if (n.opcode() == IRETURN) {
                        FieldMemberNode fmn = (FieldMemberNode) n.layer(IADD, IADD, IADD, ISHL, IALOAD, GETFIELD);
                        if (fmn != null && fmn.owner().equals(cn.name)) {
                            hooks.put("appearance", new FieldHook("appearance", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }
}

