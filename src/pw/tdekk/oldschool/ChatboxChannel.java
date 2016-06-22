package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import pw.tdekk.mod.hooks.FieldHook;
import org.objectweb.asm.tree.ClassNode;

/**
 * @author Tyler Sedlar
 */
@VisitorInfo(hooks = {"messages", "index"})
public class ChatboxChannel extends GraphVisitor {

    @Override
    public boolean validate(ClassNode cn) {
        return cn.fieldCount("[" + desc("ChatboxMessage")) > 0;
    }

    @Override
    public void visit() {
        addHook(new FieldHook("messages", cn.getField(null, "[" + desc("ChatboxMessage"))));
        addHook(new FieldHook("index", cn.getField(null, "I")));
    }
}
