package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author Tyler Sedlar
 */
@VisitorInfo(hooks = {})
public class Varpbit extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.superName.equals(clazz("CacheableNode")) && cn.getFieldTypeCount() == 1 && cn.fieldCount("I") == 3;
    }

    @Override
    public void visit() {
    }
}
