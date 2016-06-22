package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import org.objectweb.asm.tree.ClassNode;

@VisitorInfo(hooks = {})
public class Shell extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.name.equals(updater.classnodes.get("client").superName);
    }

    @Override
    public void visit() {
    }
}
