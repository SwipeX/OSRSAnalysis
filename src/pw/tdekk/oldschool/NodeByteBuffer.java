package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author Tim Dekker
 * @since 7/9/15
 */
@VisitorInfo(hooks = {})
public class NodeByteBuffer extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.superName.equals(clazz("Node")) && cn.getFieldTypeCount() == 2 && cn.fieldCount("[B") > 0 &&
                cn.fieldCount("I") > 0;
    }

    @Override
    public void visit() {

    }
}
