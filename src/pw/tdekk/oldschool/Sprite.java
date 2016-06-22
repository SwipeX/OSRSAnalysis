package pw.tdekk.oldschool;


import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.FieldMemberNode;
import org.objectweb.asm.commons.cfg.tree.node.MethodMemberNode;
import org.objectweb.asm.tree.ClassNode;
import pw.tdekk.mod.hooks.FieldHook;
import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;

/**
 * @author Tyler Sedlar
 * @since 3/11/15.
 */
@VisitorInfo(hooks = {"pixels", "width", "height"})
public class Sprite extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return !cn.ownerless() && cn.getFieldTypeCount() == 2 && cn.fieldCount("[I") == 1;
    }

    @Override
    public void visit() {
        visitAll(new PixelHooks());
    }

    private class PixelHooks extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor() {
                public void visitMethod(MethodMemberNode mmn) {
                    if (mmn.opcode() == INVOKESTATIC && mmn.desc().startsWith("([III")) {
                        FieldMemberNode pixels = mmn.firstField();
                        if (pixels == null || !pixels.owner().equals(cn.name)) {
                            return;
                        }
                        FieldMemberNode width = pixels.nextField();
                        if (width == null || !width.owner().equals(cn.name)) {
                            return;
                        }
                        FieldMemberNode height = width.nextField();
                        if (height == null || !height.owner().equals(cn.name)) {
                            return;
                        }
                        addHook(new FieldHook("pixels", pixels.fin()));
                        addHook(new FieldHook("width", width.fin()));
                        addHook(new FieldHook("height", height.fin()));
                        lock.set(true);
                    }
                }
            });
        }
    }
}
