package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import pw.tdekk.mod.hooks.FieldHook;
import pw.tdekk.util.ArrayIterator;
import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.*;
import org.objectweb.asm.tree.ClassNode;

@VisitorInfo(hooks = {"moving", "sequence", "scalar", "speedX", "speedY", "speedZ", "slope", "spawnTick", "targetDistance",
        "x", "startX", "y", "startY", "heightOffset", "height", "rotationX", "rotationY"})
public class Projectile extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.superName.equals(clazz("RenderableNode")) && cn.getFieldTypeCount() == 4 &&
                cn.fieldCount("Z") == 1 && cn.fieldCount(desc("AnimationSequence")) == 1;
    }

    @Override
    public void visit() {
        add("moving", cn.getField(null, "Z"), "Z");
        add("sequence", cn.getField(null, desc("AnimationSequence")));
        visit(new DirectionalSpeedHooks());
        visit(new AngularSpeedHooks());
        visit(new SpawnTick());
        visit(new TargetDistance());
        visit(new StartPositionHooks());
        visit(new CurrentPositionHooks());
        visit(new HeightOffset());
        visit(new Height());
        visit(new RotationX());
        visit(new RotationY());
    }

    private class DirectionalSpeedHooks extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTFIELD && fmn.owner().equals(cn.name) && fmn.desc().equals("D")) {
                        FieldMemberNode scalar = fmn;
                        MethodMemberNode mmn = fmn.firstMethod();
                        if (mmn != null && mmn.name().equals("sqrt")) {
                            AbstractNode n = mmn.first(DADD);
                            if (n != null) {
                                n = n.first(DMUL);
                                FieldMemberNode speedY = n.firstField();
                                if (speedY != null && speedY.opcode() == GETFIELD && speedY.desc().equals("D")) {
                                    n = n.next(DMUL);
                                    if (n != null) {
                                        FieldMemberNode speedX = n.firstField();
                                        if (speedX != null && speedX.opcode() == GETFIELD && speedX.desc().equals("D")) {
                                            hooks.put("scalar", new FieldHook("scalar", scalar.fin()));
                                            hooks.put("speedY", new FieldHook("speedY", speedY.fin()));
                                            hooks.put("speedX", new FieldHook("speedX", speedX.fin()));
                                            lock.set(true);
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

    private class AngularSpeedHooks extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTFIELD && fmn.owner().equals(cn.name) && fmn.desc().equals("D")) {
                        FieldMemberNode speedZ = fmn;
                        MethodMemberNode mmn = (MethodMemberNode) fmn.layer(DMUL, INVOKESTATIC);
                        if (mmn != null && mmn.name().equals("tan")) {
                            fmn = (FieldMemberNode) mmn.layer(DMUL, I2D, IMUL, GETFIELD);
                            if (fmn != null && fmn.desc().equals("I")) {
                                hooks.put("speedZ", new FieldHook("speedZ", speedZ.fin()));
                                hooks.put("slope", new FieldHook("slope", fmn.fin()));
                                lock.set(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private class SpawnTick extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitVariable(VariableNode vn) {
                    if (vn.opcode() == DSTORE) {
                        FieldMemberNode fmn = (FieldMemberNode) vn.layer(I2D, ISUB, IADD, IMUL, GETFIELD);
                        if (fmn != null && fmn.desc().equals("I")) {
                            addHook(new FieldHook("spawnTick", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class TargetDistance extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitOperation(ArithmeticNode an) {
                    if (an.opcode() == DDIV) {
                        FieldMemberNode fmn = (FieldMemberNode) an.layer(DMUL, I2D, IMUL, GETFIELD);
                        if (fmn != null && fmn.desc().equals("I")) {
                            hooks.put("targetDistance", new FieldHook("targetDistance", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class StartPositionHooks extends BlockVisitor {

        private int added = 0;

        @Override
        public boolean validate() {
            return added < 2;
        }

        @Override
        public void visit(Block block) {
            if (block.owner.name.equals("<init>")) {
                block.tree().accept(new NodeVisitor() {
                    public void visitField(FieldMemberNode fmn) {
                        if (fmn.opcode() == PUTFIELD && fmn.owner().equals(cn.name)) {
                            VariableNode vn = (VariableNode) fmn.layer(IMUL, ILOAD);
                            if (vn != null) {
                                String name = null;
                                if (vn.var() == 3) {
                                    name = "startX";
                                } else if (vn.var() == 4) {
                                    name = "startY";
                                }
                                if (name == null || hooks.containsKey(name)) {
                                    return;
                                }
                                addHook(new FieldHook(name, fmn.fin()));
                                added++;
                            }
                        }
                    }
                });
            }
        }
    }

    private class CurrentPositionHooks extends BlockVisitor {

        private ArrayIterator<String> names = new ArrayIterator<>("x", "y");

        @Override
        public boolean validate() {
            return names.hasNext();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTFIELD && fmn.owner().equals(cn.name) && fmn.desc().equals("D")) {
                        FieldMemberNode start = (FieldMemberNode) fmn.layer(DADD, I2D, IMUL, GETFIELD);
                        if (start != null && start.opcode() == GETFIELD && start.desc().equals("I")) {
                            String current = names.next();
                            if (current == null) {
                                return;
                            }
                            addHook(new FieldHook(current, fmn.fin()));
                        }
                    }
                }
            });
        }
    }

    private class HeightOffset extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTFIELD && fmn.owner().equals(cn.name) && fmn.desc().equals("D")) {
                        NumberNode nn = (NumberNode) fmn.layer(DDIV, DMUL, LDC);
                        if (nn != null && nn.number() == 2) {
                            hooks.put("heightOffset", new FieldHook("heightOffset", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class Height extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTFIELD && fmn.owner().equals(cn.name) && fmn.desc().equals("D")) {
                        fmn = (FieldMemberNode) fmn.layer(I2D, IMUL, GETFIELD);
                        if (fmn != null && fmn.desc().equals("I")) {
                            hooks.put("height", new FieldHook("height", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class RotationX extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTFIELD && fmn.owner().equals(cn.name) && fmn.desc().equals("I")) {
                        MethodMemberNode mmn = (MethodMemberNode) fmn.layer(IMUL, IAND, IADD, D2I, DMUL, INVOKESTATIC);
                        if (mmn != null && mmn.name().equals("atan2")) {
                            hooks.put("rotationX", new FieldHook("rotationX", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class RotationY extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTFIELD && fmn.owner().equals(cn.name) && fmn.desc().equals("I")) {
                        MethodMemberNode mmn = (MethodMemberNode) fmn.layer(IMUL, IAND, D2I, DMUL, INVOKESTATIC);
                        if (mmn != null && mmn.name().equals("atan2")) {
                            hooks.put("rotationY", new FieldHook("rotationY", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }
}