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

/**
 * @author Tyler Sedlar
 */
@VisitorInfo(hooks = {"type", "sender", "clan", "text", "spawnTick"})
public class ChatboxMessage extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.superName.equals(clazz("CacheableNode")) && cn.fieldCount("Ljava/lang/String;") == 3;
    }

    @Override
    public void visit() {
        visit(new Type());
        visit(new StringHooks());
        visit(new SpawnTick());
    }

    private class Type extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            if (block.owner.name.equals("<init>")) {
                block.tree().accept(new NodeVisitor() {
                    public void visitField(FieldMemberNode fmn) {
                        if (fmn.opcode() == PUTFIELD && fmn.desc().equals("I")) {
                            VariableNode vn = (VariableNode) fmn.layer(IMUL, ILOAD);
                            if (vn != null && vn.var() == 1) {
                                addHook(new FieldHook("type", fmn.fin()));
                                lock.set(true);
                            }
                        }
                    }
                });
            }
        }
    }

    private class StringHooks extends BlockVisitor {

        private int added = 0;

        @Override
        public boolean validate() {
            return added < 3;
        }

        @Override
        public void visit(Block block) {
            if (block.owner.name.equals("<init>")) {
                block.tree().accept(new NodeVisitor() {
                    public void visitField(FieldMemberNode fmn) {
                        if (fmn.opcode() == PUTFIELD && fmn.desc().equals("Ljava/lang/String;")) {
                            VariableNode vn = fmn.firstVariable();
                            if (vn.var() == 0) {
                                vn = vn.nextVariable();
                            }
                            if (vn == null) {
                                return;
                            }
                            String name = null;
                            if (vn.var() == 2) {
                                name = "sender";
                            } else if (vn.var() == 3) {
                                name = "clan";
                            } else if (vn.var() == 4) {
                                name = "text";
                            }
                            if (name == null || hooks.containsKey(name)) {
                                return;
                            }
                            addHook(new FieldHook(name, fmn.fin()));
                            added++;
                        }
                    }
                });
            }
        }
    }

    private class SpawnTick extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            if (block.owner.name.equals("<init>")) {
                block.tree().accept(new NodeVisitor() {
                    public void visitField(FieldMemberNode fmn) {
                        if (fmn.opcode() == PUTFIELD && fmn.desc().equals("I")) {
                            if (fmn.layer(IMUL, GETSTATIC) != null) {
                                addHook(new FieldHook("spawnTick", fmn.fin()));
                                lock.set(true);
                            }
                        }
                    }
                });
            }
        }
    }
}
