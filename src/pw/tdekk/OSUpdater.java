package pw.tdekk;


import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.graph.FlowGraph;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.NumberNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import pw.tdekk.oldschool.*;
import pw.tdekk.oldschool.Character;
import pw.tdekk.util.Configuration;
import pw.tdekk.visitor.GraphVisitor;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;

/**
 * @author Tyler Sedlar
 */
public class OSUpdater extends Updater {

    private static boolean server = new File("/usr/share/nginx/html/data/").exists();

    private static GraphVisitor[] createVisitors() {
        return new GraphVisitor[]{
                new Node(), new CacheableNode(), new RenderableNode(), new HashTable(),
                new NodeDeque(), new Queue(), new Cache(), new NodeByteBuffer(), new Tile(), new AnimationSequence(),
                new Character(), new NpcDefinition(), new Npc(), new Player(), new PlayerDefinition(),
                new Projectile(), new Item(), new ItemDefinition(), new ItemLayer(), new InteractableObject(),
                new ObjectDefinition(), new Region(), new Friend(), new IgnoredPlayer(), new ClientData(),
                new ClanMember(), new Canvas(), new Boundary(), new AnimableGameObject(), new FloorDecoration(),
                new WallDecoration(), new WidgetNode(), new Widget(), new WidgetActionNode(), new Varpbit(),
                new Varpbits(), new World(), new ChatboxMessage(), new ChatboxChannel(), new Sprite(),
                new ExchangeOffer(), new ItemContainer(), new Client(), new Shell()
        };
    }

    @Override
    public String getType() {
        return "Oldschool RuneScape";
    }

    @Override
    public String getHash() {
        try (JarFile jar = new JarFile(file)) {
            return Integer.toString(jar.getManifest().hashCode());
        } catch (IOException | NullPointerException e) {
            return file.getName().replace(".jar", "");
        }
    }

    @Override
    public String getAccessorPrefix() {
        return "pw/tdekk/internal/accessors/oldschool/RS";
    }

    @Override
    public String getWrapperPrefix() {
        return "pw/tdekk/api/wrapper/RS";
    }

    @Override
    public String getCallbackRouter() {
        return "pw/tdekk/internal/CallbackRouter";
    }

    @Override
    public String getModscriptLocation() {
        return server ? "/usr/share/nginx/html/data/oldschool.dat" : Configuration.CACHE + "/oldschool.dat";
    }

    @Override
    public int getRevision(Map<ClassNode, Map<MethodNode, FlowGraph>> graphs) {
        ClassNode client = classnodes.get("client");
        MethodNode init = client.getMethodByName("init");
        FlowGraph graph = graphs.get(client).get(init);
        final AtomicInteger revision = new AtomicInteger(0);
        for (Block block : graph) {
            new BlockVisitor() {
                public boolean validate() {
                    return revision.get() == 0;
                }

                public void visit(Block block) {
                    block.tree().accept(new NodeVisitor(this) {
                        public void visitNumber(NumberNode nn) {
                            if (nn != null && nn.opcode() == SIPUSH) {
                                if ((nn = nn.nextNumber()) != null && nn.opcode() == SIPUSH) {
                                    if ((nn = nn.nextNumber()) != null) {
                                        revision.set(nn.number());
                                    }
                                }
                            }
                        }
                    });
                }
            }.visit(block);
        }
        return revision.get();
    }

    static {
        Configuration.setup();
    }

    public OSUpdater(File file, GraphVisitor[] visitors, boolean closeOnOld) throws Exception {
        super(file, visitors, closeOnOld);
    }

    public OSUpdater(File file, boolean closeOnOld) throws Exception {
        this(file, createVisitors(), closeOnOld);
    }

    public static void main(String[] args) throws Exception {
        Updater updater = new OSUpdater(null, false);
//        Updater updater = new OSUpdater(new File("83.jar"), false);
        updater.print = true;
        updater.run();
    }
}
