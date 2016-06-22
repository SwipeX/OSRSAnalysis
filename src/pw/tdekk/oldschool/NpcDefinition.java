package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import pw.tdekk.mod.hooks.FieldHook;
import pw.tdekk.mod.hooks.InvokeHook;
import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

@VisitorInfo(hooks = {"name", "actions", "id", "transformIds", "transformIndex", "level", "transform"})
public class NpcDefinition extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.superName.equals(clazz("CacheableNode")) && cn.fieldCount("Z") >= 4 && cn.fieldCount("Z") < 7;
    }

    @Override
    public void visit() {
        visitTransform();
        add("name", cn.getField(null, "Ljava/lang/String;"), "Ljava/lang/String;");
        add("actions", cn.getField(null, "[Ljava/lang/String;"), "[Ljava/lang/String;");
        visitAll(new Id());
        visitAll(new TransformIds());
        visitAll(new TransformIndex());
        visitAll(new Level());
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
                public void visitMethod(MethodMemberNode mmn) {
                    FieldMemberNode cache = mmn.firstField();
                    if (cache != null && cache.desc().equals(desc("Cache"))) {
                        FieldMemberNode id = (FieldMemberNode) mmn.layer(I2L, IMUL, GETFIELD);
                        if (id != null && id.owner().equals(cn.name)) {
                            addHook(new FieldHook("id", id.fin()));
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
                public void visitMethod(MethodMemberNode mmn) {
                    if (mmn.opcode() == INVOKESTATIC && mmn.desc().startsWith("(I")) {
                        FieldMemberNode fmn = (FieldMemberNode) mmn.layer(IALOAD, GETFIELD);
                        if (fmn != null && fmn.owner().equals(cn.name) && fmn.desc().equals("[I")) {
                            if (fmn.first(ALOAD) != null) {
                                hooks.put("transformIds", new FieldHook("transformIds", fmn.fin()));
                                lock.set(true);
                            }
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

    private class Level extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor() {
                public void visitJump(JumpNode jn) {
                    AbstractNode an = jn.layer(IMUL, GETFIELD, ALOAD);
                    if (an != null) {
                        FieldMemberNode fmn = (FieldMemberNode) an.parent();
                        if (fmn.owner().equals(cn.name) && jn.layer(IMUL, GETFIELD, GETSTATIC) != null) {
                            addHook(new FieldHook("level", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }
}
