package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author Tyler Sedlar
 */
@VisitorInfo(hooks = {})
public class WidgetActionNode extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.superName.equals(clazz("Node")) && cn.fieldCount(desc("Widget")) == 2;
    }

    @Override
    public void visit() {

    }
}
