package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import pw.tdekk.mod.hooks.FieldHook;
import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.AbstractNode;
import org.objectweb.asm.commons.cfg.tree.node.FieldMemberNode;
import org.objectweb.asm.commons.cfg.tree.node.JumpNode;
import org.objectweb.asm.tree.ClassNode;

@VisitorInfo(hooks = {"name", "previousName"})
public class IgnoredPlayer extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.ownerless() && cn.getFieldTypeCount() == 1 && cn.fieldCount("Ljava/lang/String;") == 2;
    }

    @Override
    public void visit() {
        visitAll(new Name());
        visitAll(new PreviousName());
    }

    private class Name extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visit(AbstractNode n) {
                    if (n.opcode() == AASTORE) {
                        FieldMemberNode fmn = (FieldMemberNode) n.first(GETFIELD);
                        if (fmn != null && fmn.opcode() == GETFIELD && fmn.owner().equals(cn.name) &&
                                fmn.desc().equals("Ljava/lang/String;")) {
                            hooks.put("name", new FieldHook("name", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class PreviousName extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitJump(JumpNode jn) {
                    FieldMemberNode fmn = jn.firstField();
                    if (fmn != null && fmn.opcode() == GETFIELD && fmn.owner().equals(cn.name) &&
                            fmn.desc().equals("Ljava/lang/String;")) {
                        hooks.put("previousName", new FieldHook("previousName", fmn.fin()));
                        lock.set(true);
                    }
                }
            });
        }
    }
}