package pw.tdekk;

import org.objectweb.asm.commons.util.JarArchive;
import org.objectweb.asm.commons.wrapper.ClassFactory;
import org.objectweb.asm.commons.wrapper.ClassMethod;
import org.objectweb.asm.tree.ClassNode;
import pw.tdekk.deob.transform.UnusedMethodTransform;
import pw.tdekk.deob.transform.UnusedParametersTransform;
import pw.tdekk.util.io.Crawler;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by TimD on 6/23/2016.
 */
public class Deobfuscator {
    public  File file;
    public  String hash;
    public JarArchive archive;
    public Map<String, ClassNode> classnodes;

    public void start(File file){
        if (file == null) {
            Crawler crawler = new Crawler();
            file = new File("os_pack.jar");
        }
        this.file = file;
        this.archive = new JarArchive(file);
        this.classnodes = archive.build();

        System.out.println("======= Running deobfuscation transforms =======");
        List<ClassNode> remove = classnodes.values().stream().filter(cn -> cn.name.contains("/")).collect(Collectors.toList());
        for (ClassNode cn : remove) {
            classnodes.remove(cn.name);
        }
        Map<String, ClassFactory> factories = new HashMap<>();
        for (ClassNode cn : classnodes.values()) {
            factories.put(cn.name, new ClassFactory(cn));
        }
        UnusedMethodTransform umt = new UnusedMethodTransform() {
            public void populateEntryPoints(List<ClassMethod> entries) {
                for (ClassFactory factory : factories.values()) {
                    entries.addAll(factory.findMethods(cm -> cm.method.name.length() > 2));
                    entries.addAll(factory.findMethods(cm -> {
                        String superName = factory.node.superName;
                        return factories.containsKey(superName) && factories.get(superName).findMethod(icm ->
                                icm.method.name.equals(cm.method.name) && icm.method.desc.equals(cm.method.desc)) != null;
                    }));
                    entries.addAll(factory.findMethods(cm -> {
                        for (String iface : factory.node.interfaces) {
                            if (factories.containsKey(iface)) {
                                ClassFactory impl = factories.get(iface);
                                if (impl.findMethod(icm -> icm.method.name.equals(cm.method.name) &&
                                        icm.method.desc.equals(cm.method.desc)) != null) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    }));
                }
            }
        };
        umt.transform(classnodes);
        new UnusedParametersTransform().transform(classnodes);
        archive.write(new File("test.jar"));
    }

    public static void main(String[] args) {
        new Deobfuscator().start(null);
    }
}
