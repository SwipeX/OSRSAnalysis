package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import org.objectweb.asm.tree.ClassNode;

@VisitorInfo(hooks = {})
public class ClientData extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.ownerless() && cn.getFieldTypeCount() == 0 && cn.fieldCount("Ljava/lang/String;", false) >= 7 &&
                cn.fieldCount("Z", false) >= 1 && cn.fieldCount("[I", false) >= 1;
    }

    @Override
    public void visit() {
    }
}
