package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import pw.tdekk.mod.hooks.FieldHook;
import pw.tdekk.mod.hooks.InvokeHook;
import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.AbstractNode;
import org.objectweb.asm.commons.cfg.tree.node.FieldMemberNode;
import org.objectweb.asm.commons.cfg.tree.node.VariableNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

@VisitorInfo(hooks = {"name", "actions", "id", "transformIds", "transformIndex", "transform"})
public class ObjectDefinition extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.getFieldTypeCount() == 6 && cn.fieldCount("[S") == 4 && cn.fieldCount("[I") == 4;
    }

    @Override
    public void visit() {
        visitTransform();
        add("name", cn.getField(null, "Ljava/lang/String;"), "Ljava/lang/String;");
        add("actions", cn.getField(null, "[Ljava/lang/String;"), "[Ljava/lang/String;");
        visit(new Id());
        visit(new TransformIds());
        visit(new TransformIndex());
    }

    private void visitTransform() {
        for (MethodNode mn : cn.methods) {
            if ((mn.access & ACC_STATIC) == 0 && mn.desc.endsWith(")L" + cn.name + ";")) {
                addHook(new InvokeHook("transform", mn));
                break;
            }
        }
    }

    private class Id extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == GETFIELD && fmn.owner().equals(cn.name) && fmn.desc().equals("I")) {
                        if (fmn.preLayer(IMUL, ISHL, IADD, IADD, I2L) != null) {
                            hooks.put("id", new FieldHook("id", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class TransformIds extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visit(AbstractNode n) {
                    if (n.opcode() == ARETURN) {
                        FieldMemberNode fmn = (FieldMemberNode) n.layer(INVOKESTATIC, IALOAD, GETFIELD);
                        if (fmn != null && fmn.owner().equals(cn.name) && fmn.desc().equals("[I")) {
                            hooks.put("transformIds", new FieldHook("transformIds", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class TransformIndex extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(final Block block) {
            block.tree().accept(new NodeVisitor() {
                public void visitVariable(VariableNode vn) {
                    if (vn.opcode() == ISTORE && vn.var() == 2) {
                        FieldMemberNode fmn = (FieldMemberNode) vn.layer(INVOKESTATIC, IMUL, GETFIELD);
                        if (fmn != null && fmn.owner().equals(cn.name) && fmn.first(ALOAD) != null) {
                            addHook(new FieldHook("transformIndex", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }
}
