package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import pw.tdekk.mod.hooks.FieldHook;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.query.MemberQuery;
import org.objectweb.asm.commons.cfg.query.NumberQuery;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.*;
import org.objectweb.asm.commons.util.Assembly;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@VisitorInfo(hooks = {"name", "id", "stackSizes", "stackIds", "actions", "groundActions", "members", "storeValue",
        "notedId", "unnotedId"})
public class ItemDefinition extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.superName.equals(clazz("CacheableNode")) && cn.getFieldTypeCount() == 6 &&
                cn.fieldCount("[Ljava/lang/String;") == 2;
    }

    @Override
    public void visit() {
        add("name", cn.getField(null, "Ljava/lang/String;"), "Ljava/lang/String;");
        visitAll(new Id());
        visit(new StackSizes());
        visit(new StackIds());
        visit(new ActionHooks());
        visit(new Members());
        visitAll(new NotedIds());
        visitAll(new StoreValue());
    }

    private class Id extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitMethod(MethodMemberNode rn) {
                    if (rn.opcode() == INVOKESTATIC && rn.desc().startsWith("(Ljava/lang/String;Ljava/lang/String;II")) {
                        NumberNode nn = rn.firstNumber();
                        if (nn != null && nn.number() == 1005) {
                            FieldMemberNode fmn = (FieldMemberNode) rn.layer(IMUL, GETFIELD);
                            if (fmn != null && fmn.owner().equals(cn.name)) {
                                hooks.put("id", new FieldHook("id", fmn.fin()));
                                lock.set(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private class StackSizes extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitJump(JumpNode jn) {
                    if (jn.opcode() == IF_ICMPLT) {
                        FieldMemberNode fmn = (FieldMemberNode) jn.layer(IALOAD, GETFIELD);
                        if (fmn != null && fmn.owner().equals(cn.name) && fmn.desc().equals("[I")) {
                            hooks.put("stackSizes", new FieldHook("stackSizes", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class StackIds extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitVariable(VariableNode vn) {
                    if (vn.opcode() == ISTORE) {
                        FieldMemberNode fmn = (FieldMemberNode) vn.layer(IALOAD, GETFIELD);
                        if (fmn != null && fmn.owner().equals(cn.name) && fmn.desc().equals("[I")) {
                            hooks.put("stackIds", new FieldHook("stackIds", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class ActionHooks extends BlockVisitor {

        private int added = 0;

        @Override
        public boolean validate() {
            return added < 2;
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == GETFIELD && fmn.owner().equals(cn.name) && fmn.desc().equals("[Ljava/lang/String;")) {
                        AbstractNode parent = fmn.parent();
                        if (parent != null && parent.opcode() == AASTORE) {
                            NumberNode nn = (NumberNode) parent.layer(ISUB, BIPUSH);
                            if (nn != null && parent.first(INVOKEVIRTUAL) != null) {
                                String name;
                                if (nn.number() == 30) {
                                    name = "groundActions";
                                } else if (nn.number() == 35) {
                                    name = "actions";
                                } else {
                                    return;
                                }
                                if (hooks.containsKey(name)) {
                                    return;
                                }
                                addHook(new FieldHook(name, fmn.fin()));
                                added++;
                            }
                        }
                    }
                }
            });
        }
    }

    private class Members extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTFIELD && fmn.owner().equals(cn.name) && fmn.desc().equals("Z")) {
                        if (fmn.layer(GETFIELD, ALOAD) != null) {
                            hooks.put("members", new FieldHook("members", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class NotedIds extends BlockVisitor {

        private final MemberQuery fieldQuery = new MemberQuery(GETFIELD, cn.name, "I");
        private final List<Block> blocks = new ArrayList<>();

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            int idx = block.index;
            block = block.followedBlock();
            block.setIndex(idx);
            int gets = block.count(fieldQuery);
            int invoke = block.count(new MemberQuery(INVOKESTATIC, ".*" + desc("ItemDefinition") + "$"));
            int aloads = block.count(new NumberQuery(ALOAD, 2));
            if (gets >= 2 && invoke > 0 && aloads >= 3) {
                blocks.add(block);
            }
        }

        @Override
        public void visitEnd() {
            if (blocks.isEmpty()) {
                return;
            }
            blocks.sort((a, b) -> a.index - b.index);
            Block block = blocks.get(0);
            FieldInsnNode notedId = (FieldInsnNode) block.get(fieldQuery, 0);
            FieldInsnNode unnotedId = (FieldInsnNode) block.get(fieldQuery, 1);
            addHook(new FieldHook("notedId", notedId));
            addHook(new FieldHook("unnotedId", unnotedId));
            lock.set(true);
        }
    }

    private class StoreValue extends MethodVisitor {

        private AtomicBoolean lock = new AtomicBoolean(false);

        @Override
        public void visitFieldInsn(FieldInsnNode fin) {
            if (lock.get() || !fin.owner.equals(cn.name) || !fin.desc.equals("I") || hooks.containsKey("storeValue")) {
                return;
            }
            AbstractInsnNode ain = Assembly.next(fin, a -> {
                if (a instanceof IntInsnNode) {
                    IntInsnNode iin = (IntInsnNode) a;
                    return iin.operand == 4204;
                }
                return false;
            });
            if (ain != null) {
                addHook(new FieldHook("storeValue", fin));
            }
        }
    }
}
