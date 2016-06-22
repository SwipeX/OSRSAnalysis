package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import org.objectweb.asm.tree.ClassNode;

@VisitorInfo(hooks = {"definition"})
public class Npc extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.fieldCount(desc("NpcDefinition")) == 1;
    }

    @Override
    public void visit() {
        add("definition", cn.getField(null, desc("NpcDefinition")));
    }
}
