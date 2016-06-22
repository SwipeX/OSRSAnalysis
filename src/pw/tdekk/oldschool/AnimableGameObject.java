package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import org.objectweb.asm.tree.ClassNode;

@VisitorInfo(hooks = {"sequence", "transformed"})
public class AnimableGameObject extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.superName.equals(clazz("RenderableNode")) & cn.fieldCount(desc("AnimationSequence")) == 1 &&
                cn.fieldCount("Z") == 1 && cn.fieldCount("I") == 8;
    }

    @Override
    public void visit() {
        add("sequence", cn.getField(null, desc("AnimationSequence")));
        add("transformed", cn.getField(null, "Z"), "Z");
    }
}
