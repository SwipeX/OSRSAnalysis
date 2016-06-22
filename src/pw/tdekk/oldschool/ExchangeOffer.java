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

import java.util.LinkedList;
import java.util.List;

/**
 * @author Tim Dekker
 * @since 4/13/15
 */
@VisitorInfo(hooks = {"status", "spent", "id", "amount", "transferred", "price"})
public class ExchangeOffer extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.ownerless() && cn.getFieldTypeCount() == 2 && cn.fieldCount("B") == 1 && cn.fieldCount("I") > 3;
    }

    @Override
    public void visit() {
        add("status", cn.getField(null, "B"));
        visitAll(new SpentHooks());
        visitAll(new PriceHooks());
    }

    private class SpentHooks extends BlockVisitor {

        private int added = 0;

        @Override
        public boolean validate() {
            return added < 3;
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor() {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTFIELD && fmn.owner().equals(cn.name) && fmn.desc().equals("I")) {
                        FieldMemberNode child = fmn.firstField();
                        if (child != null && child.desc().equals("L" + cn.name + ";")) {
                            VariableNode vn = (VariableNode) fmn.layer(IMUL, ILOAD);
                            if (vn != null) {
                                String name = null;
                                if (vn.var() == 3) {
                                    name = "id";
                                } else if (vn.var() == 4) {
                                    name = "price";
                                } else if (vn.var() == 5) {
                                    name = "amount";
                                }
                                if (name == null || hooks.containsKey(name)) {
                                    return;
                                }
                                added++;
                                addHook(new FieldHook(name, fmn.fin()));
                            }
                        }
                    }
                }
            });
        }
    }

    private class PriceHooks extends BlockVisitor {

        public List<FieldMemberNode> fields = new LinkedList<>();

        @Override
        public boolean validate() {
            return fields.size() <= 2;
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor() {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTFIELD && fmn.owner().equals(cn.name) && fmn.desc().equals("I")) {
                        FieldMemberNode child = fmn.firstField();
                        if (child != null && child.opcode() == GETFIELD && child.desc().equals("L" + cn.name + ";")) {
                            if (fmn.first(ICONST_0) != null) {
                                fields.add(fmn);
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void visitEnd() {
            if (fields.size() == 2) {
                FieldMemberNode a = fields.get(0), b = fields.get(1);
                if (a.index() < b.index()) {
                    addHook(new FieldHook("transferred", a.fin()));
                    addHook(new FieldHook("spent", b.fin()));
                } else {
                    addHook(new FieldHook("transferred", b.fin()));
                    addHook(new FieldHook("spent", a.fin()));
                }
            }
        }
    }
}
