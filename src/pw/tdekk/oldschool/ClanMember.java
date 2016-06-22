package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import pw.tdekk.mod.hooks.FieldHook;
import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.FieldMemberNode;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author Tyler Sedlar
 * @since 4/8/15.
 */
@VisitorInfo(hooks = {"name", "world", "rank"})
public class ClanMember extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.superName.equals(clazz("Node")) && cn.fieldCount("Ljava/lang/String;") == 2 &&
                cn.fieldCount("B") > 0 && cn.fieldCount("I") > 0;
    }

    @Override
    public void visit() {
        visitAll(new Name());
    }

    private class Name extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTFIELD && fmn.owner().equals(cn.name) && fmn.desc().equals("Ljava/lang/String;")) {
                        FieldMemberNode name = (FieldMemberNode) fmn.layer(INVOKESTATIC, GETFIELD);
                        if (name != null && name.opcode() == GETFIELD && name.owner().equals(cn.name) &&
                                name.desc().equals("Ljava/lang/String;")) {
                            if (name.layer(AALOAD, ALOAD) != null) {
                                FieldMemberNode world = fmn.nextField();
                                if (world != null && world.opcode() == PUTFIELD && world.owner().equals(cn.name) &&
                                        world.desc().equals("I")) {
                                    FieldMemberNode rank = world.nextField();
                                    if (rank != null && rank.opcode() == PUTFIELD && rank.owner().equals(cn.name) &&
                                            rank.desc().equals("B")) {
                                        addHook(new FieldHook("name", name.fin()));
                                        addHook(new FieldHook("world", world.fin()));
                                        addHook(new FieldHook("rank", rank.fin()));
                                        lock.set(true);
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
