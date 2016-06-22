package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import pw.tdekk.mod.hooks.FieldHook;
import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.FieldMemberNode;
import org.objectweb.asm.commons.cfg.tree.node.JumpNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

@VisitorInfo(hooks = {"tail", "head"})
public class NodeDeque extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.ownerless() && cn.fields.size() == 2 && cn.fieldCount(desc("Node")) == 2;
    }

    @Override
    public void visit() {
        visit(new NodeHooks());
    }

    private class NodeHooks extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitJump(JumpNode jn) {
                    if (jn.opcode() == IF_ACMPNE) {
                        FieldMemberNode fmn = jn.firstField();
                        String node = desc("Node");
                        if (fmn != null && fmn.desc().equals(node)) {
                            hooks.put("tail", new FieldHook("tail", fmn.fin()));
                            for (FieldNode fn : cn.fields) {
                                if (fn.desc.equals(node)) {
                                    if (!fn.name.equals(fmn.name())) {
                                        hooks.put("head", new FieldHook("head", fn));
                                        break;
                                    }
                                }
                            }
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }
}
