package pw.tdekk;

import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import pw.tdekk.deob.CallGraph;
import pw.tdekk.util.Crawler;

import java.io.IOException;

/**
 * Created by TimD on 6/16/2016.
 */
public class Application {
    public static JarArchive archive;
    private static int fremoved;

    public static void main(String[] args) throws IOException {
        Crawler crawler = new Crawler();
        if (crawler.outdated()) {
            crawler.download();
        }
        archive = new JarArchive("os_pack.jar");
        archive.build();
        int mCount = 0;
        int fCount = 0;

        for (ClassNode classNode : archive.classes().values()) {
            mCount += classNode.methods.size();
            fCount += classNode.fields.size();
        }
        CallGraph callGraph = new CallGraph();
        callGraph.build();
        int removed = 0;
        for (Handle handle : callGraph.getCalledMethods()) {
            ClassNode node = archive.classes().get(handle.getOwner());
            if (node != null) {
                MethodNode method = node.getMethod(handle.getName(), handle.getDesc());
                node.methods.remove(method);
                removed++;
            }
        }
        for (Handle handle : callGraph.getCalledFields()) {
            ClassNode node = archive.classes().get(handle.getOwner());
            if (node != null) {
                FieldNode field = node.getField(handle.getName(), handle.getDesc());
                node.fields.remove(field);
                fremoved++;
            }
        }
        System.out.println("Removed " + (mCount - removed) + " unused methods");
        System.out.println("Removed " + (fCount - fremoved) + " unused fields");
    }
}
