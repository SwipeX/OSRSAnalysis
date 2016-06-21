package pw.tdekk;

import org.objectweb.asm.ClassWriter;
import pw.tdekk.deob.AbstractTransform;
import pw.tdekk.deob.OpaquePredicates;
import pw.tdekk.deob.UnusedMethods;
import pw.tdekk.util.Crawler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by TimD on 6/16/2016.
 */
public class Application {
    public static JarArchive archive;
    public static int methodCount;
    public static int fieldCount;
    private final static AbstractTransform[] transforms = new AbstractTransform[]{new UnusedMethods(), new OpaquePredicates()};

    public static void main(String[] args) throws IOException, InterruptedException {
        Crawler crawler = new Crawler();
        if (crawler.outdated()) {
            crawler.download();
        }
        archive = new JarArchive("os_pack.jar");
        archive.build();
        while(!archive.built){Thread.sleep(20);}
        Arrays.stream(transforms).forEach(t -> t.transform());
        archive.write(new File("test.jar"), ClassWriter.COMPUTE_MAXS);
    }
}
