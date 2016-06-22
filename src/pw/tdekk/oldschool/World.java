package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import pw.tdekk.mod.hooks.FieldHook;
import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.FieldMemberNode;
import org.objectweb.asm.commons.cfg.tree.node.MethodMemberNode;
import org.objectweb.asm.commons.cfg.tree.node.VariableNode;
import org.objectweb.asm.tree.ClassNode;

@VisitorInfo(hooks = {"world", "mask", "domain", "activity", "serverLocation", "playerCount"})
public class World extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.ownerless() && cn.fieldCount("Ljava/lang/String;") == 2 && cn.fieldCount("I") == 5;
    }

    @Override
    public void visit() {
        visitAll(new WorldHooks());
    }

    private class WorldHooks extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitVariable(VariableNode vn) {
                    if (vn.opcode() == ASTORE) {
                        MethodMemberNode mmn = (MethodMemberNode) vn.layer(AASTORE, DUP_X2, INVOKESPECIAL);
                        String world = clazz("World");
                        if (mmn != null && mmn.owner().equals(world)) {
                            FieldMemberNode worldId = vn.nextField();
                            if (worldId != null && worldId.owner().equals(world) && worldId.desc().equals("I")) {
                                FieldMemberNode mask = worldId.nextField();
                                if (mask != null && mask.owner().equals(world) && mask.desc().equals("I")) {
                                    FieldMemberNode domain = mask.nextField();
                                    if (domain != null && domain.desc().equals("Ljava/lang/String;")) {
                                        FieldMemberNode activity = domain.nextField();
                                        if (activity != null && activity.desc().equals("Ljava/lang/String;")) {
                                            FieldMemberNode location = activity.nextField();
                                            if (location != null && location.desc().equals("I")) {
                                                FieldMemberNode count = location.nextField();
                                                if (count != null && count.desc().equals("I")) {
                                                    addHook(new FieldHook("world", worldId.fin()));
                                                    addHook(new FieldHook("mask", mask.fin()));
                                                    addHook(new FieldHook("domain", domain.fin()));
                                                    addHook(new FieldHook("activity", activity.fin()));
                                                    addHook(new FieldHook("serverLocation", location.fin()));
                                                    addHook(new FieldHook("playerCount", count.fin()));
                                                    lock.set(true);
                                                }
                                            }
                                        }
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
