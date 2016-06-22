package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import pw.tdekk.mod.hooks.FieldHook;
import pw.tdekk.util.ArrayIterator;
import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.FieldMemberNode;
import org.objectweb.asm.commons.cfg.tree.node.JumpNode;
import org.objectweb.asm.commons.cfg.tree.node.MethodMemberNode;
import org.objectweb.asm.commons.cfg.tree.node.VariableNode;
import org.objectweb.asm.tree.ClassNode;

@VisitorInfo(hooks = {"definition", "name", "prayerIcon", "skullIcon", "level", "team"})
public class Player extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.superName.equals(clazz("Character")) && cn.fieldCount("Ljava/lang/String;") >= 1 &&
                cn.fieldCount("Z") >= 1 && cn.getAbnormalFieldCount() == 2;
    }

    @Override
    public void visit() {
        add("definition", cn.getField(null, desc("PlayerDefinition")));
        add("name", cn.getField(null, "Ljava/lang/String;"));
        visitAll(new IconHooks());
        visitAll(new Level());
        visitAll(new Team());
    }

    private class IconHooks extends BlockVisitor {

        private final ArrayIterator<String> itr = new ArrayIterator<>("prayerIcon", "skullIcon");

        @Override
        public boolean validate() {
            return itr.hasNext();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitMethod(MethodMemberNode mmn) {
                    if (mmn.opcode() == INVOKEVIRTUAL && mmn.desc().startsWith("(I")) {
                        FieldMemberNode fmn = (FieldMemberNode) mmn.layer(AALOAD, IMUL, GETFIELD);
                        if (fmn != null && fmn.owner().equals(cn.name) && fmn.desc().equals("I")) {
                            addHook(new FieldHook(itr.next(), fmn.fin()));
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
            block.tree().accept(new NodeVisitor(this) {
                public void visitVariable(VariableNode vn) {
                    if (vn.opcode() == ISTORE && vn.var() == 8) {
                        FieldMemberNode level = (FieldMemberNode) vn.layer(IMUL, GETFIELD);
                        if (level != null && level.owner().equals(cn.name) && level.desc().equals("I")) {
                            FieldMemberNode rn = level.firstField();
                            if (rn != null && rn.desc().equals("L" + cn.name + ";")) {
                                hooks.put("level", new FieldHook("level", level.fin()));
                                lock.set(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private class Team extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitJump(JumpNode jn) {
                    FieldMemberNode fmn = (FieldMemberNode) jn.layer(IMUL, GETFIELD);
                    if (fmn != null && fmn.owner().equals(cn.name)) {
                        FieldMemberNode player = fmn.firstField();
                        if (player != null && player.opcode() == GETSTATIC && player.desc().equals("L" + cn.name + ";")) {
                            addHook(new FieldHook("team", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }
}
