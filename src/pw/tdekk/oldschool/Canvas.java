package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import org.objectweb.asm.tree.ClassNode;

@VisitorInfo(hooks = {"component"})
public class Canvas extends GraphVisitor {

    @Override
    public String iface() {
        return updater.getAccessorPackage() + "/input/RSCanvas";
    }

    @Override
    public boolean validate(ClassNode cn) {
        return cn.superName.equals("java/awt/Canvas") && cn.fieldCount("Ljava/awt/Component;") == 1;
    }

    @Override
    public void visit() {
        add("component", cn.getField(null, "Ljava/awt/Component;"), "Ljava/awt/Component;");
    }
}
