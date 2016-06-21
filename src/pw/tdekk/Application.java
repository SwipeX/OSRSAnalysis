package pw.tdekk;


import org.objectweb.asm.tree.ClassNode;
import pw.tdekk.deob.Mutator;
import pw.tdekk.deob.UnusedMethods;
import pw.tdekk.util.Archive;
import pw.tdekk.util.Crawler;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;


/**
 * Created by TimD on 6/16/2016.
 */
public class Application {
    private static JarFile OSRS;
    private static Mutator[] mutators = new Mutator[]{new UnusedMethods()};
    private static ConcurrentHashMap<String, ClassNode> classes;

    public static void main(String[] args) {
        try {
            Crawler crawler = new Crawler();
            if (crawler.outdated()) {
                crawler.download();
            }
            OSRS = new JarFile(new File("os_pack.jar"));
            long startTime = System.currentTimeMillis();
            classes = Archive.build(OSRS);
            Arrays.stream(mutators).forEach(Mutator::mutate);
            System.out.println("Executed in: " + (System.currentTimeMillis() - startTime));
            Archive.write(new File("test.jar"), classes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ConcurrentHashMap<String, ClassNode> getClasses() {
        return classes;
    }
}
