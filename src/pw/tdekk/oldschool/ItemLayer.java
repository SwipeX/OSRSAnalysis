package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import pw.tdekk.mod.hooks.FieldHook;
import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.FieldMemberNode;
import org.objectweb.asm.commons.cfg.tree.node.VariableNode;
import org.objectweb.asm.tree.ClassNode;

@VisitorInfo(hooks = {"x", "y", "plane", "id", "height", "bottom", "middle", "top"})
public class ItemLayer extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.ownerless() && cn.getFieldTypeCount() == 2 && cn.fieldCount(desc("RenderableNode")) == 3;
    }

    @Override
    public void visit() {
        visit("Region", new LayerHooks());
    }

    private class LayerHooks extends BlockVisitor {

        private int added = 0;

        @Override
        public boolean validate() {
            return added < 8;
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTFIELD && fmn.owner().equals(cn.name)) {
                        if (fmn.desc().equals("I")) {
                            VariableNode vn = (VariableNode) fmn.layer(IMUL, ILOAD);
                            if (vn == null) {
                                vn = (VariableNode) fmn.layer(IADD, IMUL, ILOAD);
                            }
                            if (vn != null) {
                                String name = null;
                                if (vn.var() == 2) {
                                    name = "x";
                                } else if (vn.var() == 3) {
                                    name = "y";
                                } else if (vn.var() == 4) {
                                    name = "plane";
                                } else if (vn.var() == 6) {
                                    name = "id";
                                } else if (vn.var() == 10) {
                                    name = "height";
                                }
                                if (name == null) {
                                    return;
                                }
                                hooks.put(name, new FieldHook(name, fmn.fin()));
                                added++;
                            }
                        } else if (fmn.desc().equals(desc("RenderableNode"))) {
                            VariableNode vn = fmn.firstVariable();
                            if (vn != null) {
                                vn = vn.nextVariable();
                            }
                            if (vn != null) {
                                String name = null;
                                if (vn.var() == 5) {
                                    name = "bottom";
                                } else if (vn.var() == 7) {
                                    name = "middle";
                                } else if (vn.var() == 8) {
                                    name = "top";
                                }
                                if (name == null) {
                                    return;
                                }
                                hooks.put(name, new FieldHook(name, fmn.fin()));
                                added++;
                            }
                        }
                    }
                }
            });
        }
    }
}

