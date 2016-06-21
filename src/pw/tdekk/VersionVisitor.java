package pw.tdekk;

/**
 * Created by TimD on 6/21/2016.
 */

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class VersionVisitor extends MethodVisitor {
    private int state = 0;
    private int version = -1;

    VersionVisitor() {
        super(Opcodes.ASM5);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        if (state == 2) {
            version = operand;
            ++state;
        }

        if (operand == 765 || operand == 503) {
            ++state;
        }
    }

    public int getVersion() {
        return version;
    }
}