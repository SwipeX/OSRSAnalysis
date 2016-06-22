package pw.tdekk;

import pw.tdekk.util.Configuration;
import pw.tdekk.util.StringMatcher;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.cfg.query.MemberQuery;
import org.objectweb.asm.commons.util.JarArchive;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Tyler Sedlar
 * @since 6/22/2015
 */
public class UsageFinder implements Opcodes {

    public static void main(String[] args) {
        UsageFinder analyzer = new UsageFinder(new File(Configuration.CACHE + "os_pack.jar"));
        analyzer.printFieldUsages("eh", "fj", false, true);
        //analyzer.printMethodUsages("b", "dz", null);
    }

    private final Map<String, ClassNode> classes;

    public UsageFinder(File jar) {
        this.classes = new JarArchive(jar).build();
    }

    public void printFieldUsages(String owner, String field, boolean getters, boolean setters) {
        Set<String> usages = new TreeSet<>();
        MemberQuery getField = new MemberQuery(GETFIELD, owner, field, null);
        MemberQuery getStatic = new MemberQuery(GETSTATIC, owner, field, null);
        MemberQuery putField = new MemberQuery(PUTFIELD, owner, field, null);
        MemberQuery putStatic = new MemberQuery(PUTSTATIC, owner, field, null);
        for (ClassNode cn : classes.values()) {
            for (MethodNode mn : cn.methods) {
                if (getters) {
                    int count = mn.count(getField) + mn.count(getStatic);
                    if (count > 0) {
                        usages.add(String.format("%s.%s%s - %s (GETTERS)", cn.name, mn.name, mn.desc, count));
                    }
                }
                if (setters) {
                    int count = mn.count(putField) + mn.count(putStatic);
                    if (count > 0) {
                        usages.add(String.format("%s.%s%s - %s (SETTERS)", cn.name, mn.name, mn.desc, count));
                    }
                }
            }
        }
        System.out.println("--------------------------------------------------");
        System.out.println(String.format("printFieldUsages(\"%s\", \"%s\", %s, %s):", owner, field, getters, setters));
        System.out.println("--------------------------------------------------");
        usages.forEach(System.out::println);
        System.out.println("--------------------------------------------------");
    }

    public void printMethodUsages(String owner, String method, String desc) {
        Set<String> usages = new TreeSet<>();
        for (ClassNode cn : classes.values()) {
            for (MethodNode mn : cn.methods) {
                String mnKey = cn.name + "." + mn.name + mn.desc;
                for (AbstractInsnNode ain : mn.instructions.toArray()) {
                    if (ain instanceof MethodInsnNode) {
                        MethodInsnNode min = (MethodInsnNode) ain;
                        if (owner == null || StringMatcher.matches(owner, min.owner)) {
                            if (method == null || StringMatcher.matches(method, min.name)) {
                                if (desc == null || StringMatcher.matches(desc, min.desc)) {
                                    String minKey = min.owner + "." + min.name + min.desc;
                                    usages.add(minKey + " @ " + mnKey);
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("--------------------------------------------------");
        System.out.println(String.format("printMethodUsages(\"%s\", \"%s\", \"%s\")", owner, method, desc));
        System.out.println("--------------------------------------------------");
        usages.forEach(System.out::println);
        System.out.println("--------------------------------------------------");
    }
}
