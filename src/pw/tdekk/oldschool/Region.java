package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import pw.tdekk.mod.hooks.FieldHook;
import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.FieldMemberNode;
import org.objectweb.asm.commons.cfg.tree.node.JumpNode;
import org.objectweb.asm.commons.cfg.tree.node.VariableNode;
import org.objectweb.asm.tree.ClassNode;

@VisitorInfo(hooks = {"tiles", "objects", "width", "height"})
public class Region extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.ownerless() && cn.getAbnormalFieldCount() == 2 &&
                cn.fieldCount("[[[" + desc("Tile")) == 1 && cn.fieldCount("[" + desc("InteractableObject")) == 1;
    }

    @Override
    public void visit() {
        add("tiles", cn.getField(null, "[[[" + desc("Tile")));
        add("objects", cn.getField(null, "[" + desc("InteractableObject")));
        visit(new SizeHooks());
    }

    private class SizeHooks extends BlockVisitor {

        private int added = 0;

        @Override
        public boolean validate() {
            return added < 2;
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitJump(JumpNode jn) {
                    VariableNode vn = jn.firstVariable();
                    if (vn != null && (vn.var() == 14 || vn.var() == 15)) {
                        FieldMemberNode fmn = jn.firstField();
                        if (fmn != null && fmn.owner().equals(cn.name) && fmn.desc().equals("I")) {
                            if (fmn.first(ALOAD) != null) {
                                if (jn.opcode() == IF_ICMPLT) {
                                    if (!hooks.containsKey("width")) {
                                        hooks.put("width", new FieldHook("width", fmn.fin()));
                                        added++;
                                    }
                                } else if (jn.opcode() == IF_ICMPGE) {
                                    if (!hooks.containsKey("height")) {
                                        hooks.put("height", new FieldHook("height", fmn.fin()));
                                        added++;
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }
}

