package pw.tdekk;

import org.objectweb.asm.tree.ClassNode;
import pw.tdekk.deob.UnusedVisitor;
import pw.tdekk.util.Crawler;

import java.io.IOException;

/**
 * Created by TimD on 6/16/2016.
 */
public class Application {
    public static JarArchive archive;

    public static void main(String[] args) throws IOException {
        Crawler crawler = new Crawler();
        if (crawler.outdated()) {
            crawler.download();
        }
        archive = new JarArchive("os_pack.jar");
        archive.build();
        archive.dispatch(new UnusedVisitor());
        for(ClassNode cn :archive.classes().values()){
            cn.methods.stream().filter(mn -> mn.referenceCount == 0).forEach(mn ->
                    System.out.println(cn.name + "." + mn.name + "_" + mn.desc + " Possible removal"));
        }
    }
}
