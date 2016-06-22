package pw.tdekk.oldschool;

import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.visitor.VisitorInfo;
import pw.tdekk.mod.hooks.CodeHook;
import pw.tdekk.mod.hooks.FieldHook;
import pw.tdekk.mod.hooks.InvokeHook;
import pw.tdekk.util.ArrayIterator;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.cfg.Block;
import org.objectweb.asm.commons.cfg.BlockVisitor;
import org.objectweb.asm.commons.cfg.query.InsnQuery;
import org.objectweb.asm.commons.cfg.query.MemberQuery;
import org.objectweb.asm.commons.cfg.query.NumberQuery;
import org.objectweb.asm.commons.cfg.tree.NodeTree;
import org.objectweb.asm.commons.cfg.tree.NodeVisitor;
import org.objectweb.asm.commons.cfg.tree.node.*;
import org.objectweb.asm.commons.cfg.tree.util.TreeBuilder;
import org.objectweb.asm.commons.util.Assembly;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@VisitorInfo(hooks = {"username", "levels", "realLevels", "experiences", "fps", "menuX",
        "menuWidth", "menuHeight", "menuY", "menuActions", "menuOptions", "menuVisible", "menuOptionCount",
        "loginState", "cameraZ", "cameraX", "cameraY", "plane", "cameraPitch", "cameraYaw",
        "mapAngle", "mapOffset", "mapScale", "hintX", "baseX", "hintY", "baseY",
        "varps", "cachedVarps", "gameState", "world", "selectedItemName",
        "widgetPositionsX", "widgetPositionsY", "widgetWidths", "widgetHeights",
        "players", "nodeDeque", "npcs", "hashTable", "friends",
        "canvas", "player", "region", "widgets", "objects", "groundItems", "worlds",
        "renderRules", "tileHeights", "widgetNodes", "currentTick", "playerIndex",
        "setGameState", "itemDefinitionCache", "objectDefinitionCache",
        "chatboxChannels", "loadObjectDefinition", "loadNpcDefinition", "loadItemDefinition", "doAction",
        "selectionState", "loadSprite", "sendChatboxMessage", "exchangeOffers", "npcIndices", "projectiles",
        "viewportWidth", "viewportHeight", "viewportScale", "hoveredRegionTileX", "hoveredRegionTileY",
        "ignoredPlayers", "clanMembers", "energy", "weight", "itemContainers", "outdatedWidgets",
        "experienceGained", "itemContainerChanged", "characterMoved", "messageReceived", "groundItemLoaded",
        "projectileLoaded", "playerAnimationChanged", "npcAnimationChanged", "varpChanged", "offerUpdated",
        "resizable"})
public class Client extends GraphVisitor {

    @Override
    public String iface() {
        return updater.getAccessorPackage() + "/Client";
    }

    @Override
    public boolean validate(ClassNode cn) {
        return cn.name.equals("client");
    }

    private void visitDefLoader(String hook, String visitor, boolean transform) {
        for (ClassNode cn : updater.classnodes.values()) {
            cn.methods.stream().filter(mn -> mn.desc.endsWith(")" + desc(visitor))).forEach(mn -> {
                int access = mn.access & ACC_STATIC;
                if (transform ? access == 0 : access > 0) {
                    addHook(new InvokeHook(hook, cn.name, mn.name, mn.desc));
                }
            });
        }
    }

    @Override
    public void visit() {
        visitStaticFields();
        visitAll(new ExperienceHooks());
        visitAll(new Fps());
        visitAll(new MenuPositionHooks());
        visitAll(new MenuStrings());
        visitAll(new MenuVisible());
        visitAll(new MenuOptionCount());
        visitAll(new LoginState());
        visitAll(new Username());
        visitAll(new CameraXY());
        visitAll(new CameraZ());
        visitAll(new CameraPY());
        visitAll(new Plane());
        visitAll(new MapHooks());
        visitAll(new MapAngle());
        visit("Varpbits", new SettingHooks());
        visitAll(new GameState());
        visitAll(new World());
        visitAll(new SelectedItemName());
        visitAll(new WidgetPositionHooks());
        visitAll(new RenderRules());
        visitAll(new TileHeights());
        visitAll(new Projectiles());
        visitAll(new WidgetNodes());
        visitAll(new CurrentTick());
        visitAll(new HintHooks());
        visitAll(new PlayerIndex());
        visitAll(new ItemDefinitionCache());
        visitAll(new ObjectDefinitionCache());
        visitAll(new SelectionState());
        visitAll(new NpcIndices());
        visitAll(new ChatboxChannels());
        visitAll(new ViewportHooks());
        visitAll(new HoveredRegionTiles());
        visitAll(new RunHooks());
        visitAll(new ViewportScale());
        visitAll(new OutdatedWidgets());
        visitAll(new Resizable());
        visitSetGameState();
        visitDoAction();
        visitLoadSprite();
        visitSendChatboxMessage();
        visitDefLoader("loadObjectDefinition", "ObjectDefinition", false);
        visitDefLoader("loadNpcDefinition", "NpcDefinition", false);
        visitDefLoader("loadItemDefinition", "ItemDefinition", false);
        visitExpCallback();
        visitInvCallback();
        visitCharCallback();
        visitMessageCallback();
//        visitGroundCallback();
        visitProjectileCallback();
        visitPlayerAnimationCallback();
        visitNpcAnimationCallback();
        visitVarpCallback();
        visitExchangeCallback();
    }

    private void visitExchangeCallback() {
        FieldHook status = (FieldHook) updater.visitor("ExchangeOffer").hooks.get("status");
        String exchangeOffer = clazz("ExchangeOffer");
        ClassNode offer = updater.classnodes.get(exchangeOffer);
        MethodNode method = offer.getMethod("<init>", "(" + desc("NodeByteBuffer") + "Z)V");
        if (method != null) {
            TreeBuilder.build(method).accept(new NodeVisitor() {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn != null && fmn.name().equals(status.field) && fmn.opcode() == PUTFIELD) {
                        InsnList insn = new InsnList();
                        insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        insn.add(new MethodInsnNode(Opcodes.INVOKESTATIC, updater.getCallbackRouter(),
                                "offerUpdated", "(L" + updater.visitor("ExchangeOffer").iface() + ";)V", false));
                        addHook(new CodeHook("offerUpdated", method.owner.name, method.name,
                                method.desc, fmn.index(), insn));
                    }
                }
            });
        }
    }

    private void visitVarpCallback() {
        MethodNode method = Assembly.findMethod(mn -> (mn.access & ACC_STATIC) != 0 &&
                mn.desc.matches(reg("\\(I[PRED]\\)V")) && methodHas("cachedVarps", mn), updater.classnodes);
        if (method != null) {
            InsnList insn = new InsnList();
            insn.add(new VarInsnNode(Opcodes.ILOAD, 0));
            insn.add(new MethodInsnNode(Opcodes.INVOKESTATIC, updater.getCallbackRouter(),
                    "varpChanged", "(I)V", false));
            addHook(new CodeHook("varpChanged", method.owner.name, method.name, method.desc, 0, insn));
        }
    }

    private void visitNpcAnimationCallback() {
        FieldHook animation = (FieldHook) updater.visitor("Character").hooks.get("animation");
        if (animation == null) return;
        List<MethodNode> methods = Assembly.findMethods(mn -> mn.desc.equals("(I)V"), updater.classnodes);
        AtomicBoolean found = new AtomicBoolean(false);
        for (MethodNode mn : methods) {
            if (found.get())
                return;
            TreeBuilder.build(mn).accept(new NodeVisitor() {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn != null && fmn.name().equals(animation.field) && fmn.opcode() == PUTFIELD) {
                        VarInsnNode animation = Assembly.previous(fmn.fin(), ILOAD);
                        if (animation != null) {
                            VarInsnNode npc = Assembly.previous(animation, ALOAD);
                            if (npc != null) {
                                InsnList insn = new InsnList();
                                insn.add(new VarInsnNode(Opcodes.ALOAD, npc.var));
                                insn.add(new VarInsnNode(Opcodes.ILOAD, animation.var));
                                insn.add(new MethodInsnNode(Opcodes.INVOKESTATIC, updater.getCallbackRouter(),
                                        "animationChanged", "(L" + updater.visitor("Character").iface() + ";I)V",
                                        false));
                                addHook(new CodeHook("npcAnimationChanged", mn.owner.name, mn.name, mn.desc,
                                        npc.index() - 1, insn));
                                found.set(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private void visitPlayerAnimationCallback() {
        FieldHook animation = (FieldHook) updater.visitor("Character").hooks.get("animation");
        if (animation == null) return;
        List<MethodNode> methods = Assembly.findMethods(mn -> (mn.access & ACC_STATIC) != 0 &&
                mn.desc.startsWith("(" + desc("Player") + "II"), updater.classnodes);
        AtomicBoolean found = new AtomicBoolean(false);
        for (MethodNode mn : methods) {
            if (found.get())
                return;
            TreeBuilder.build(mn).accept(new NodeVisitor() {
                @Override
                public void visitField(FieldMemberNode fmn) {
                    if (fmn != null && fmn.name().equals(animation.field) && fmn.opcode() == PUTFIELD) {
                        int start = 0;
                        for (AbstractInsnNode ain : mn.instructions.toArray()) {
                            if (ain.opcode() != -1) { //locate first valid instruction
                                start = ain.index();
                                break;
                            }
                        }
                        InsnList insn = new InsnList();
                        insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        insn.add(new VarInsnNode(Opcodes.ILOAD, 1));
                        insn.add(new MethodInsnNode(Opcodes.INVOKESTATIC, updater.getCallbackRouter(),
                                "animationChanged", "(L" + updater.visitor("Character").iface() + ";I)V", false));
                        addHook(new CodeHook("playerAnimationChanged", mn.owner.name, mn.name, mn.desc, start, insn));
                        found.set(true);
                    }
                }
            });
        }
    }

    private void visitProjectileCallback() {
        List<MethodNode> methods = Assembly.findMethods(mn -> (mn.access & ACC_STATIC) != 0 &&
                mn.desc.equals("(I)V"), updater.classnodes);
        AtomicBoolean found = new AtomicBoolean(false);
        for (MethodNode mn : methods) {
            if (found.get())
                return;
            TreeBuilder.build(mn).accept(new NodeVisitor() {
                public void visitMethod(MethodMemberNode mmn) {
                    if (mmn.opcode() == INVOKEVIRTUAL && mmn.owner().equals(clazz("NodeDeque"))) {
                        FieldMemberNode fmn = (FieldMemberNode) mmn.first(GETSTATIC);
                        if (fmn != null && fmn.key().equals(getHookKey("projectiles"))) {
                            VariableNode load = (VariableNode) mmn.first(ALOAD);
                            if (load != null) {
                                InsnList insn = new InsnList();
                                insn.add(new VarInsnNode(Opcodes.ALOAD, load.var()));
                                insn.add(new MethodInsnNode(Opcodes.INVOKESTATIC, updater.getCallbackRouter(),
                                        "projectileLoaded", "(L" + updater.visitor("Projectile").iface() + ";)V", false));
                                addHook(new CodeHook("projectileLoaded", mn.owner.name, mn.name, mn.desc, mmn.index(), insn));
                                found.set(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private void visitGroundCallback() {
        List<MethodNode> methods = Assembly.findMethods(mn -> mn.desc.equals("(I)V"), updater.classnodes);
        for (MethodNode mn : methods) {
            TreeBuilder.build(mn).accept(
                    new NodeVisitor() {

                        private VariableNode item, xLoad, yLoad;
                        private int idx = 0;

                        @Override
                        public void visitVariable(VariableNode vn) {
                            if (vn.opcode() == ASTORE) {
                                MethodMemberNode invoke = vn.firstMethod();
                                if (invoke != null && invoke.owner().equals(clazz("Item")) &&
                                        invoke.opcode() == INVOKESPECIAL) {
                                    item = vn;
                                }
                            }
                        }

                        @Override
                        public void visitMethod(MethodMemberNode mmn) {
                            if (mmn.desc().equals("(" + desc("Node") + ")V") && mmn.owner().equals(clazz("NodeDeque"))) {
                                MethodMemberNode m2 = mmn.nextMethod(12);
                                if (m2 != null && !m2.owner().equals(mmn.owner()) && !m2.name().equals(mmn.name())) {
                                    idx = m2.index();
                                }
                            }
                        }

                        @Override
                        public void visit(AbstractNode an) {
                            if (an.opcode() == AASTORE) {
                                FieldMemberNode fmn = (FieldMemberNode) an.layer(AALOAD, AALOAD, GETSTATIC);
                                if (fmn != null && fmn.desc().equals("[[[" + desc("NodeDeque"))) {
                                    VariableNode loadA = (VariableNode) fmn.parent().parent().first(ILOAD);
                                    if (loadA != null) {
                                        VariableNode loadB = (VariableNode) an.layer(INVOKESPECIAL, DUP, ILOAD);
                                        if (loadB != null) {
                                            MethodMemberNode deque = (MethodMemberNode) loadB.parent().parent();
                                            if (deque.owner().equals(clazz("NodeDeque"))) {
                                                xLoad = loadA;
                                                yLoad = loadB;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public void visitEnd() {
                            if (item != null && xLoad != null && yLoad != null) {
                                InsnList insn = new InsnList();
                                insn.add(new VarInsnNode(Opcodes.ALOAD, item.var()));
                                insn.add(new VarInsnNode(Opcodes.ILOAD, xLoad.var()));
                                insn.add(new VarInsnNode(Opcodes.ILOAD, yLoad.var()));
                                insn.add(new MethodInsnNode(Opcodes.INVOKESTATIC, updater.getCallbackRouter(),
                                        "groundItemLoaded", "(L" + updater.visitor("Item").iface() + ";II)V", false));
                                addHook(new CodeHook("groundItemLoaded", mn.owner.name, mn.name, mn.desc, idx, insn));
                            }
                        }
                    }
            );
        }
    }

    private void visitMessageCallback() {
        List<MethodNode> methods = Assembly.findMethods(mn -> (mn.access & ACC_STATIC) != 0 &&
                mn.desc.startsWith("(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;") &&
                mn.desc.endsWith("V"), updater.classnodes);
        if (methods != null && methods.size() > 0) {
            MethodNode method = methods.get(0);
            InsnList insn = new InsnList();
            insn.add(new VarInsnNode(Opcodes.ILOAD, 0));
            insn.add(new VarInsnNode(Opcodes.ALOAD, 1));
            insn.add(new VarInsnNode(Opcodes.ALOAD, 2));
            insn.add(new VarInsnNode(Opcodes.ALOAD, 3));
            insn.add(new MethodInsnNode(Opcodes.INVOKESTATIC, updater.getCallbackRouter(),
                    "messageReceived", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false));
            addHook(new CodeHook("messageReceived", method.owner.name, method.name, method.desc, 0, insn));
        }
    }

    private void visitCharCallback() {
        MethodNode mn = Assembly.findMethod(methodNode -> methodNode.owner.name.equals(clazz("Character")) &&
                methodNode.desc.matches(reg("\\(IZ[PRED]\\)V")), updater.classnodes);
        if (mn != null) {
            InsnList insn = new InsnList();
            FieldHook x = (FieldHook) updater.visitor("Character").hooks.get("x");
            FieldHook y = (FieldHook) updater.visitor("Character").hooks.get("y");
            insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insn.add(new FieldInsnNode(Opcodes.GETFIELD, mn.owner.name, x.field, "I"));
            insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insn.add(new FieldInsnNode(Opcodes.GETFIELD, mn.owner.name, y.field, "I"));
            insn.add(new MethodInsnNode(Opcodes.INVOKESTATIC, updater.getCallbackRouter(),
                    "characterMoved", "(L" + updater.visitor("Character").iface() + ";II)V", false));
            addHook(new CodeHook("characterMoved", mn.owner.name, mn.name, mn.desc, 0, insn));
        }
    }

    private void visitExpCallback() {
        List<MethodNode> methods = Assembly.findMethods(mn -> (mn.access & ACC_STATIC) != 0 &&
                methodHas("experiences", mn), updater.classnodes);
        if (methods != null && methods.size() > 0) {
            AtomicBoolean found = new AtomicBoolean(false);
            for (MethodNode method : methods) {
                if (found.get())
                    return;
                NodeTree tree = TreeBuilder.build(method);
                tree.accept(new NodeVisitor() {
                    public void visit(AbstractNode an) {
                        if (an.opcode() == IASTORE) {
                            List<AbstractNode> loads = an.findChildren(ILOAD);
                            if (loads != null && loads.size() == 2) {
                                FieldMemberNode fmn = an.firstField();
                                if (fmn != null && fmn.key().equals(getHookKey("experiences"))) {
                                    Collections.sort(loads, (a, b) -> {
                                        VariableNode vA = (VariableNode) a;
                                        VariableNode vB = (VariableNode) b;
                                        return vA.index() - vB.index();
                                    });
                                    VariableNode first = (VariableNode) loads.get(0);
                                    VariableNode second = (VariableNode) loads.get(1);
                                    InsnList inject = new InsnList();
                                    inject.add(new VarInsnNode(ILOAD, first.var()));
                                    inject.add(new VarInsnNode(ILOAD, second.var()));
                                    inject.add(new MethodInsnNode(INVOKESTATIC, updater.getCallbackRouter(),
                                            "experienceGained", "(II)V", false));
                                    addHook(new CodeHook("experienceGained", method, fmn.index() - 1, inject));
                                    found.set(true);
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    private void visitInvCallback() {
        List<MethodNode> methods = Assembly.findMethods(mn -> (mn.access & ACC_STATIC) > 0 &&
                mn.desc.matches(reg("\\(IIII[PRED]\\)V")), updater.classnodes);
        if (methods != null && methods.size() > 0) {
            for (MethodNode method : methods) {
                if (Assembly.count(CHECKCAST, method) == 1) {
                    InsnList insn = new InsnList();
                    insn.add(new VarInsnNode(Opcodes.ILOAD, 0));
                    insn.add(new VarInsnNode(Opcodes.ILOAD, 1));
                    insn.add(new VarInsnNode(Opcodes.ILOAD, 2));
                    insn.add(new VarInsnNode(Opcodes.ILOAD, 3));
                    insn.add(new MethodInsnNode(Opcodes.INVOKESTATIC, updater.getCallbackRouter(),
                            "itemContainerChanged", "(IIII)V", false));
                    addHook(new CodeHook("itemContainerChanged", method, 0, insn));
                    return;
                }
            }
        }
    }

    private void visitStaticFields() {
        add("players", cn.getField(null, "[" + desc("Player"), false));
        add("nodeDeque", cn.getField(null, desc("NodeDeque"), false));
        add("npcs", cn.getField(null, "[" + desc("Npc"), false));
        add("hashTable", cn.getField(null, desc("HashTable"), false));
        add("friends", cn.getField(null, "[" + desc("Friend"), false));
        add("ignoredPlayers", cn.getField(null, "[" + desc("IgnoredPlayer"), false));
        add("exchangeOffers", cn.getField(null, "[" + desc("ExchangeOffer"), false));
        String playerDesc = desc("Player");
        String regionDesc = desc("Region");
        String widgetDesc = desc("Widget");
        String objectDesc = desc("InteractableObject");
        String dequeDesc = desc("NodeDeque");
        String worldDesc = desc("World");
        String clanDesc = desc("ClanMember");
        for (ClassNode node : updater.classnodes.values()) {
            for (FieldNode fn : node.fields) {
                if ((fn.access & Opcodes.ACC_STATIC) == 0) {
                    continue;
                }
                if (fn.desc.equals("Ljava/awt/Canvas;")) {
                    add("canvas", fn);
                } else if (playerDesc != null && fn.desc.equals(playerDesc)) {
                    add("player", fn);
                } else if (regionDesc != null && fn.desc.equals(regionDesc)) {
                    add("region", fn);
                } else if (widgetDesc != null && fn.desc.equals("[[" + widgetDesc)) {
                    add("widgets", fn);
                } else if (objectDesc != null && fn.desc.equals("[" + objectDesc)) {
                    add("objects", fn);
                } else if (dequeDesc != null && fn.desc.equals("[[[" + dequeDesc)) {
                    add("groundItems", fn);
                } else if (worldDesc != null && fn.desc.equals("[" + worldDesc)) {
                    add("worlds", fn);
                } else if (clanDesc != null && fn.desc.equals("[" + clanDesc)) {
                    add("clanMembers", fn);
                }
            }
        }
    }

    private void visitSetGameState() {
        FieldHook state = (FieldHook) hooks.get("gameState");
        if (state == null) {
            return;
        }
        for (ClassNode cn : updater.classnodes.values()) {
            cn.methods.stream().filter(mn -> (mn.access & ACC_STATIC) > 0 && mn.desc.startsWith("(I") && mn.desc.endsWith(")V") &&
                    mn.parameters() <= 2).forEach(mn -> {
                int count = mn.count(new MemberQuery(PUTSTATIC, state.clazz, state.field, null));
                int count2 = mn.count(new MemberQuery(PUTSTATIC, "I"));
                if (count > 0 && count2 > 3) {
                    addHook(new InvokeHook("setGameState", mn));
                }
            });
        }
    }

    private void visitDoAction() {
        for (ClassNode cn : updater.classnodes.values()) {
            cn.methods.stream().filter(mn -> mn.desc.startsWith("(IIIILjava/lang/String;Ljava/lang/String;II") &&
                    mn.desc.endsWith(")V")).forEach(mn -> addHook(new InvokeHook("doAction", mn)));
        }
    }

    private void visitLoadSprite() {
        for (ClassNode cn : updater.classnodes.values()) {
            cn.methods.stream().filter(mn -> mn.desc.startsWith("(IIIIIZ") &&
                    mn.desc.endsWith(")" + desc("Sprite"))).forEach(mn -> addHook(new InvokeHook("loadSprite", mn)));
        }
    }

    private void visitSendChatboxMessage() {
        for (ClassNode cn : updater.classnodes.values()) {
            cn.methods.stream().filter(mn -> {
                if ((mn.access & ACC_STATIC) == 0) {
                    return false;
                }
                String desc = mn.desc;
                return desc.startsWith("(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;") &&
                        desc.endsWith(")V");
            }).forEach(mn -> addHook(new InvokeHook("sendChatboxMessage", mn)));
        }
    }

    private class ExperienceHooks extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(final Block block) {
            if (block.count(new InsnQuery(ISTORE)) == 4 && block.count(new InsnQuery(IASTORE)) == 3) {
                NodeTree root = block.tree();
                AbstractNode storeE = root.find(IASTORE, 0);
                if (storeE == null) {
                    return;
                }
                FieldMemberNode experiences = storeE.firstField();
                if (experiences == null || experiences.opcode() != GETSTATIC) {
                    return;
                }
                AbstractNode storeL = root.find(IASTORE, 1);
                if (storeL == null) {
                    return;
                }
                FieldMemberNode levels = storeL.firstField();
                if (levels == null || levels.opcode() != GETSTATIC) {
                    return;
                }
                AbstractNode storeRL = root.find(IASTORE, 2);
                if (storeRL == null) {
                    return;
                }
                FieldMemberNode realLevels = storeRL.firstField();
                if (realLevels == null || realLevels.opcode() != GETSTATIC) {
                    return;
                }
                addHook(new FieldHook("experiences", experiences.fin()));
                addHook(new FieldHook("levels", levels.fin()));
                addHook(new FieldHook("realLevels", realLevels.fin()));
                lock.set(true);
            }
        }
    }

    private class Fps extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTSTATIC && fmn.owner().equals(clazz("Shell")) && fmn.desc().equals("I")) {
                        if (fmn.layer(IMUL, IDIV) != null) {
                            hooks.put("fps", new FieldHook("fps", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class MenuPositionHooks extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitMethod(MethodMemberNode mmn) {
                    if (mmn.opcode() == INVOKESTATIC && mmn.desc().matches(reg("\\(IIII[PRED]\\)V"))) {
                        AbstractNode xmul = mmn.find(IMUL, 0);
                        if (xmul == null) {
                            return;
                        }
                        FieldMemberNode x = xmul.firstField();
                        if (x == null || x.opcode() != GETSTATIC) {
                            return;
                        }
                        AbstractNode ymul = mmn.find(IMUL, 1);
                        if (ymul == null) {
                            return;
                        }
                        FieldMemberNode y = ymul.firstField();
                        if (y == null || y.opcode() != GETSTATIC) {
                            return;
                        }
                        AbstractNode wmul = mmn.find(IMUL, 2);
                        if (wmul == null) {
                            return;
                        }
                        FieldMemberNode w = wmul.firstField();
                        if (w == null || w.opcode() != GETSTATIC) {
                            return;
                        }
                        AbstractNode hmul = mmn.find(IMUL, 3);
                        if (hmul == null) {
                            return;
                        }
                        FieldMemberNode h = hmul.firstField();
                        if (h == null || h.opcode() != GETSTATIC) {
                            return;
                        }
                        addHook(new FieldHook("menuX", x.fin()));
                        addHook(new FieldHook("menuY", y.fin()));
                        addHook(new FieldHook("menuWidth", w.fin()));
                        addHook(new FieldHook("menuHeight", h.fin()));
                    }
                }
            });
        }
    }

    private class MenuStrings extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            if (block.count(new InsnQuery(AASTORE)) == 2 && block.count(new InsnQuery(IASTORE)) == 1) {
                NodeTree root = block.tree();
                AbstractNode iastore = root.find(IASTORE, 0);
                if (iastore == null) {
                    return;
                }
                NumberNode nn = (NumberNode) iastore.first(SIPUSH);
                if (nn == null || nn.number() != 1006) {
                    return;
                }
                AbstractNode storeA = root.find(AASTORE, 0);
                if (storeA == null) {
                    return;
                }
                FieldMemberNode actions = storeA.firstField();
                if (actions == null || actions.opcode() != GETSTATIC) {
                    return;
                }
                AbstractNode storeO = root.find(AASTORE, 1);
                if (storeO == null) {
                    return;
                }
                FieldMemberNode options = storeO.firstField();
                if (options == null || options.opcode() != GETSTATIC) {
                    return;
                }
                addHook(new FieldHook("menuActions", actions.fin()));
                addHook(new FieldHook("menuOptions", options.fin()));
                lock.set(true);
            }
        }
    }

    private class MenuVisible extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.followedTree().accept(new NodeVisitor() {
                public void visitMethod(MethodMemberNode mmn) {
                    if (mmn.opcode() == INVOKESTATIC) {
                        List<AbstractNode> nodes = mmn.layerAll(IMUL, GETSTATIC);
                        if (nodes != null && nodes.size() >= 4) {
                            FieldMemberNode fmn = (FieldMemberNode) mmn.tree().first(PUTSTATIC);
                            if (fmn != null && fmn.desc().equals("Z")) {
                                addHook(new FieldHook("menuVisible", fmn.fin()));
                                lock.set(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private class MenuOptionCount extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitJump(JumpNode jn) {
                    if (jn.opcode() == IF_ICMPGE) {
                        if (jn.first(ICONST_2) != null) {
                            FieldMemberNode fmn = (FieldMemberNode) jn.layer(IMUL, GETSTATIC);
                            if (fmn != null && fmn.owner().equals("client") && fmn.desc().equals("I")) {
                                hooks.put("menuOptionCount", new FieldHook("menuOptionCount", fmn.fin()));
                                lock.set(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private class LoginState extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitJump(JumpNode jn) {
                    if (jn.opcode() == IF_ICMPNE) {
                        NumberNode nn = jn.firstNumber();
                        if (nn != null && nn.number() >= 3) {
                            FieldMemberNode fmn = (FieldMemberNode) jn.layer(IMUL, GETSTATIC);
                            if (fmn != null && fmn.owner().equals(clazz("ClientData")) && fmn.desc().equals("I")) {
                                hooks.put("loginState", new FieldHook("loginState", fmn.fin()));
                                lock.set(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private class Username extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    String data = clazz("ClientData");
                    if (fmn.caller().name.equals(data)) {
                        return;
                    }
                    if (fmn.opcode() == PUTSTATIC && fmn.owner().equals(data) && fmn.desc().equals("Ljava/lang/String;")) {
                        FieldInsnNode fin = fmn.fin();
                        MethodMemberNode mmn = fmn.firstMethod();
                        if (mmn != null && mmn.opcode() == INVOKEVIRTUAL && mmn.name().equals("trim")) {
                            hooks.put("username", new FieldHook("username", fin));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class CameraXY extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitJump(JumpNode jn) {
                    FieldMemberNode x = (FieldMemberNode) jn.layer(IAND, BALOAD, AALOAD, ISHR, IMUL, GETSTATIC);
                    if (x == null) {
                        return;
                    }
                    FieldMemberNode y = (FieldMemberNode) jn.layer(IAND, BALOAD, ISHR, IMUL, GETSTATIC);
                    if (y == null) {
                        return;
                    }
                    addHook(new FieldHook("cameraX", x.fin()));
                    addHook(new FieldHook("cameraY", y.fin()));
                    lock.set(true);
                }
            });
        }
    }

    private class CameraZ extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(final Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitJump(JumpNode jn) {
                    NumberNode nn = jn.firstNumber();
                    if (nn != null && nn.number() == 800) {
                        FieldMemberNode fmn = (FieldMemberNode) jn.layer(ISUB, IMUL, GETSTATIC);
                        if (fmn != null) {
//                            System.out.println(jn.method().owner.name + "." + jn.method().name + jn.method().desc);
                            addHook(new FieldHook("cameraZ", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class CameraPY extends BlockVisitor {

        private int added = 0;

        @Override
        public boolean validate() {
            return added < 2;
        }

        @Override
        public void visit(final Block block) {
            block.tree().accept(new NodeVisitor() {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTSTATIC) {
                        NumberNode nn = (NumberNode) fmn.layer(IMUL, IAND, D2I, DMUL, LDC);
                        if (nn != null) {
                            int mul = nn.number();
                            nn = (NumberNode) fmn.layer(IMUL, IAND, SIPUSH);
                            if (nn != null && nn.number() == 0x07FF) {
                                String name = "camera" + (mul > 0 ? "Pitch" : "Yaw");
                                if (hooks.containsKey(name)) {
                                    return;
                                }
                                addHook(new FieldHook(name, fmn.fin()));
                            }
                        }
                    }
                }
            });
        }
    }

    private class Plane extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visit(AbstractNode n) {
                    if (n.opcode() == AASTORE) {
                        FieldMemberNode fmn = (FieldMemberNode) n.layer(AALOAD, AALOAD, IMUL, GETSTATIC);
                        if (fmn != null && fmn.desc().equals("I")) {
                            hooks.put("plane", new FieldHook("plane", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class MapHooks extends BlockVisitor {

        private final ArrayIterator<String> itr = new ArrayIterator<>("mapScale", "mapOffset");

        @Override
        public boolean validate() {
            return itr.hasNext();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitJump(JumpNode jn) {
                    if (jn.opcode() == IF_ICMPLE || jn.opcode() == IF_ICMPGE) {
                        int push = jn.opcode() == IF_ICMPLE ? 60 : -20;
                        NumberNode nn = jn.firstNumber();
                        if (nn != null && nn.number() == push) {
                            FieldMemberNode fmn = (FieldMemberNode) jn.layer(IMUL, GETSTATIC);
                            if (fmn != null && fmn.desc().equals("I")) {
                                int nameIdx = jn.opcode() == IF_ICMPLE ? 0 : 1;
                                addHook(new FieldHook(itr.get(nameIdx), fmn.fin()));
                            }
                        }
                    }
                }
            });
        }
    }

    private class MapAngle extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTSTATIC && fmn.desc().equals("I")) {
                        if (fmn.layer(IMUL, IAND, IADD, IDIV) != null) {
                            hooks.put("mapAngle", new FieldHook("mapAngle", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class HintHooks extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitMethod(MethodMemberNode mmn) {
                    if (mmn.opcode() == INVOKESTATIC) {
                        if (mmn.child(0) != null && mmn.child(0).opcode() == IADD) {
                            if (mmn.child(1) != null && mmn.child(1).opcode() == IADD) {
                                if (mmn.child(2) != null && mmn.child(2).opcode() == IMUL) {
                                    AbstractNode xBlock = mmn.child(0).layer(ISHL, ISUB);
                                    AbstractNode yBlock = mmn.child(1).layer(ISHL, ISUB);
                                    FieldMemberNode type = (FieldMemberNode) mmn.child(2).first(GETSTATIC);
                                    if (xBlock != null && yBlock != null && type != null) {
                                        FieldMemberNode x = (FieldMemberNode) xBlock.layer(IMUL, GETSTATIC);
                                        FieldMemberNode baseX = x.parent().next().firstField();
                                        FieldMemberNode y = (FieldMemberNode) yBlock.layer(IMUL, GETSTATIC);
                                        FieldMemberNode baseY = y.parent().next().firstField();
                                        hooks.put("hintX", new FieldHook("hintX", x.fin()));
                                        hooks.put("baseX", new FieldHook("baseX", baseX.fin()));
                                        hooks.put("hintY", new FieldHook("hintY", y.fin()));
                                        hooks.put("baseY", new FieldHook("baseY", baseY.fin()));
                                        lock.set(true);
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private class SettingHooks extends BlockVisitor {

        private final ArrayIterator<String> itr = new ArrayIterator<>("cachedVarps", "varps");

        @Override
        public boolean validate() {
            return itr.hasNext();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTSTATIC && fmn.desc().equals("[I")) {
                        NumberNode nn = (NumberNode) fmn.layer(NEWARRAY, SIPUSH);
                        if (nn != null && nn.number() == 2000) {
                            addHook(new FieldHook(itr.next(), fmn.fin()));
                        }
                    }
                }
            });
        }
    }

    private class GameState extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitJump(JumpNode jn) {
                    if (jn.opcode() == IF_ICMPNE) {
                        NumberNode nn = jn.firstNumber();
                        if (nn != null && nn.number() == 1000) {
                            FieldMemberNode fmn = (FieldMemberNode) jn.layer(IMUL, GETSTATIC);
                            if (fmn != null && fmn.owner().equals("client") && fmn.desc().equals("I")) {
                                hooks.put("gameState", new FieldHook("gameState", fmn.fin()));
                                lock.set(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private class World extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitOperation(ArithmeticNode an) {
                    if (an.opcode() == IADD) {
                        NumberNode nn = an.firstNumber();
                        if (nn != null && nn.number() == 40000) {
                            if ((an = an.firstOperation()) != null && an.opcode() == IMUL) {
                                FieldMemberNode fmn = an.firstField();
                                if (fmn != null && fmn.opcode() == GETSTATIC && fmn.desc().equals("I")) {
                                    hooks.put("world", new FieldHook("world", fmn.fin()));
                                    lock.set(true);
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private class SelectedItemName extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(final Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn != null && fmn.opcode() == PUTSTATIC && fmn.owner().equals("client") &&
                            fmn.desc().equals("Ljava/lang/String;")) {
                        ConstantNode cn = fmn.firstConstant();
                        if (cn != null && cn.cst() != null && cn.cst().equals("null")) {
                            hooks.put("selectedItemName", new FieldHook("selectedItemName", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class WidgetPositionHooks extends BlockVisitor {

        private final ArrayIterator<String> itr = new ArrayIterator<>("widgetPositionsX", "widgetPositionsY",
                "widgetWidths", "widgetHeights");

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitMethod(MethodMemberNode mmn) {
                    if (mmn.desc().startsWith("(IIIII")) {
                        AbstractNode x = mmn.child(0);
                        if (x == null || x.opcode() != IALOAD) {
                            return;
                        }
                        AbstractNode y = mmn.child(1);
                        if (y == null || y.opcode() != IALOAD) {
                            return;
                        }
                        AbstractNode w = mmn.child(2);
                        if (w == null || w.opcode() != IALOAD) {
                            return;
                        }
                        AbstractNode h = mmn.child(3);
                        if (h == null || h.opcode() != IALOAD) {
                            return;
                        }
                        AbstractNode[] parents = {x, y, w, h};
                        FieldMemberNode[] fields = new FieldMemberNode[4];
                        for (int i = 0; i < parents.length; i++) {
                            FieldMemberNode fmn = parents[i].firstField();
                            if (fmn == null || !fmn.desc().equals("[I")) {
                                return;
                            }
                            for (int j = i - 1; j > 0; j--) {
                                if (fields[j].key().equals(fmn.key())) {
                                    return;
                                }
                            }
                            fields[i] = fmn;
                        }
                        for (int i = 0; i < itr.size(); i++) {
                            hooks.put(itr.get(i), new FieldHook(itr.get(i), fields[i].fin()));
                        }
                        lock.set(true);
                    }
                }
            });
        }
    }

    private class RenderRules extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitJump(JumpNode jn) {
                    FieldMemberNode fmn = (FieldMemberNode) jn.layer(IAND, BALOAD, AALOAD, AALOAD, GETSTATIC);
                    if (fmn != null && fmn.desc().equals("[[[B")) {
                        addHook(new FieldHook("renderRules", fmn.fin()));
                        lock.set(true);
                    }
                }
            });
        }
    }

    private class TileHeights extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitVariable(VariableNode vn) {
                    FieldMemberNode fmn = (FieldMemberNode) vn.layer(ISUB, IALOAD, AALOAD, AALOAD, GETSTATIC);
                    if (fmn != null && fmn.desc().equals("[[[I")) {
                        hooks.put("tileHeights", new FieldHook("tileHeights", fmn.fin()));
                        lock.set(true);
                    }
                }
            });
        }
    }

    private class Projectiles extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor() {
                public void visitVariable(VariableNode vn) {
                    if (vn.opcode() == ASTORE) {
                        TypeNode check = vn.firstType();
                        if (check != null && check.type().equals(clazz("Projectile"))) {
                            MethodMemberNode mmn = vn.firstMethod();
                            if (mmn != null && mmn.opcode() == INVOKEVIRTUAL && mmn.desc().endsWith(desc("Node"))) {
                                FieldMemberNode fmn = mmn.firstField();
                                if (fmn != null && fmn.opcode() == GETSTATIC && fmn.desc().equals(desc("NodeDeque"))) {
                                    addHook(new FieldHook("projectiles", fmn.fin()));
                                    lock.set(true);
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private class WidgetNodes extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitVariable(VariableNode vn) {
                    if (vn.opcode() == ASTORE) {
                        TypeNode tn = vn.firstType();
                        if (tn != null && tn.type().equals(clazz("WidgetNode"))) {
                            FieldMemberNode fmn = (FieldMemberNode) vn.layer(INVOKEVIRTUAL, GETSTATIC);
                            if (fmn != null && fmn.desc().equals(desc("HashTable"))) {
                                hooks.put("widgetNodes", new FieldHook("widgetNodes", fmn.fin()));
                                lock.set(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private class CurrentTick extends BlockVisitor {

        private String ticksKey = null;

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTFIELD && fmn.desc().equals("I")) {
                        fmn = (FieldMemberNode) fmn.layer(IMUL, IAND, D2I, DMUL, INVOKESTATIC, DDIV, I2D, IMUL, GETSTATIC);
                        if (fmn != null && fmn.desc().equals("I")) {
                            ticksKey = fmn.key();
                            addHook(new FieldHook("currentTick", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }

        @Override
        public void visitEnd() {
            visitAll(new Widget.WidgetRepaintTick(updater.visitor("Widget"), ticksKey));
        }
    }

    private class PlayerIndex extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTSTATIC) {
                        fmn = (FieldMemberNode) fmn.layer(IMUL, ISHL, IMUL, GETSTATIC);
                        if (fmn != null && fmn.desc().equals("I")) {
                            hooks.put("playerIndex", new FieldHook("playerIndex", fmn.fin()));
                            lock.set(true);
                        }
                    }
                }
            });
        }
    }

    private class ItemDefinitionCache extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitVariable(VariableNode vn) {
                    if (vn.opcode() == ASTORE) {
                        TypeNode tn = vn.firstType();
                        if (tn != null && tn.type().equals(clazz("ItemDefinition"))) {
                            FieldMemberNode fmn = (FieldMemberNode) vn.layer(INVOKEVIRTUAL, GETSTATIC);
                            if (fmn != null && fmn.desc().equals(desc("Cache"))) {
                                hooks.put("itemDefinitionCache", new FieldHook("itemDefinitionCache", fmn.fin()));
                                lock.set(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private class ObjectDefinitionCache extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitVariable(VariableNode vn) {
                    if (vn.opcode() == ASTORE) {
                        TypeNode tn = vn.firstType();
                        if (tn != null && tn.type().equals(clazz("ObjectDefinition"))) {
                            FieldMemberNode fmn = (FieldMemberNode) vn.layer(INVOKEVIRTUAL, GETSTATIC);
                            if (fmn != null && fmn.desc().equals(desc("Cache"))) {
                                hooks.put("objectDefinitionCache", new FieldHook("objectDefinitionCache", fmn.fin()));
                                lock.set(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private class ChatboxChannels extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor(this) {
                public void visitVariable(VariableNode vn) {
                    if (vn.opcode() == ASTORE) {
                        TypeNode tn = vn.firstType();
                        if (tn != null && tn.type().equals(clazz("ChatboxChannel"))) {
                            FieldMemberNode fmn = (FieldMemberNode) vn.layer(INVOKEINTERFACE, GETSTATIC);
                            if (fmn != null && fmn.desc().equals("Ljava/util/Map;")) {
                                hooks.put("chatboxChannels", new FieldHook("chatboxChannels", fmn.fin()));
                                lock.set(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private class SelectionState extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            Block logical = block.followedBlock();
            NodeTree tree = logical.tree();
            VariableNode vn = (VariableNode) tree.first(ASTORE);
            if (vn != null && vn.var() == 9 && vn.layer(INVOKESTATIC, ILOAD) != null) {
                List<AbstractNode> statics = tree.findChildren(PUTSTATIC);
                List<AbstractNode> fields = tree.findChildren(PUTFIELD);
                if (statics != null && statics.size() <= 4 && (fields == null || fields.size() <= 2)) {
                    for (AbstractNode an : statics) {
                        FieldMemberNode field = (FieldMemberNode) an;
                        if (field.desc().equals("I") && field.first(LDC) != null && field.first(IMUL) == null) {
                            addHook(new FieldHook("selectionState", field.fin()));
                            lock.set(true);
                        }
                    }
                }
            }
        }
    }

    private class NpcIndices extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor() {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.parent() != null && fmn.parent().opcode() == AALOAD) {
                        if (fmn.desc().equals("[" + desc("Npc"))) {
                            fmn = (FieldMemberNode) fmn.parent().layer(IALOAD, GETSTATIC);
                            if (fmn != null && fmn.desc().equals("[I")) {
                                addHook(new FieldHook("npcIndices", fmn.fin()));
                                lock.set(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private class ViewportHooks extends BlockVisitor {

        private int added = 0;

        @Override
        public boolean validate() {
            return added < 2;
        }

        @Override
        public void visit(Block block) {
            NodeTree tree = block.followedTree();
            tree.accept(new NodeVisitor() {
                public void visitField(FieldMemberNode fmn) {
                    if (fmn.opcode() == PUTSTATIC) {
                        AbstractNode an = fmn.layer(IMUL, IADD);
                        if (an != null) {
                            List<AbstractNode> divs = an.layerAll(IDIV);
                            if (divs != null && divs.size() == 2) {
                                AbstractNode loader = null;
                                AbstractNode nonLoader = null;
                                int load = -1;
                                for (AbstractNode node : divs) {
                                    VariableNode var = (VariableNode) node.layer(IMUL, ILOAD);
                                    if (var != null) {
                                        loader = node;
                                        load = var.var();
                                    } else {
                                        nonLoader = node;
                                    }
                                }
                                if (loader != null && nonLoader != null && load != -1) {
                                    FieldMemberNode field = (FieldMemberNode) nonLoader.layer(IMUL, GETSTATIC);
                                    if (field != null) {
                                        String name;
                                        if (load == 0) {
                                            name = "viewportWidth";
                                        } else if (load == 4) {
                                            name = "viewportHeight";
                                        } else {
                                            return;
                                        }
                                        if (hooks.containsKey(name)) {
                                            return;
                                        }
                                        addHook(new FieldHook(name, field.fin()));
                                        added++;
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private class HoveredRegionTiles extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            NodeTree tree = block.followedBlock().tree();
            List<AbstractNode> layer = tree.layerAll(ISTORE, GETSTATIC);
            if (layer != null && layer.size() == 2) {
                tree.accept(new NodeVisitor() {
                    private boolean valid = false;

                    @Override
                    public void visit(AbstractNode n) {
                        if (n.opcode() == BALOAD) {
                            FieldMemberNode fmn = n.firstField();
                            if (fmn != null && fmn.desc().equals("[Z"))
                                valid = true;
                        }
                    }

                    @Override
                    public void visitEnd() {
                        if (valid) {
                            layer.sort((a, b) -> {
                                VariableNode vA = (VariableNode) a.parent();
                                VariableNode vB = (VariableNode) b.parent();
                                return vA.var() - vB.var();
                            });
                            FieldMemberNode x = (FieldMemberNode) layer.get(0);
                            FieldMemberNode y = (FieldMemberNode) layer.get(1);
                            addHook(new FieldHook("hoveredRegionTileX", x.fin()));
                            addHook(new FieldHook("hoveredRegionTileY", y.fin()));
                            lock.set(true);
                        }
                    }
                });
            }
        }
    }

    private class RunHooks extends BlockVisitor {

        private final Set<Block> blocks = new TreeSet<>();
        private final ArrayIterator<String> names = new ArrayIterator<>("energy", "weight");

        @Override
        public boolean validate() {
            return blocks.size() < 2;
        }

        @Override
        public void visit(final Block block) {
            if (block.owner.desc.matches(reg("\\(" + desc("Widget") + "I[PRED]\\)I"))) {
                if (block.count(new MemberQuery(GETSTATIC, "client", "I")) > 0) {
                    blocks.add(block);
                }
            }
        }

        @Override
        public void visitEnd() {
            Iterator<Block> itr = blocks.iterator();
            for (int i = 0; i < names.size(); i++) {
                addHook(new FieldHook(names.next(), (FieldInsnNode) itr.next().get(GETSTATIC)));
            }
        }
    }

    private class ViewportScale extends BlockVisitor {

        private int added;

        @Override
        public boolean validate() {
            return added < 3;
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor() {
                @Override
                public void visitNumber(NumberNode nn) {
                    if (nn.number() == 334) {
                        FieldMemberNode set = (FieldMemberNode) nn.preLayer(IDIV, ISHL, IMUL, PUTSTATIC);
                        if (set != null) {
                            addHook(new FieldHook("viewportScale", set.fin()));
                            added++;
                        }
                    }
                }
            });
        }
    }

    private class OutdatedWidgets extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            block.tree().accept(new NodeVisitor() {
                public void visit(AbstractNode n) {
                    if (n.opcode() == BASTORE) {
                        FieldMemberNode outdated = n.firstField();
                        if (outdated != null && outdated.opcode() == GETSTATIC && outdated.desc().equals("[Z")) {
                            FieldMemberNode fmn = (FieldMemberNode) n.layer(IMUL, GETFIELD);
                            if (fmn != null && fmn.owner().equals(clazz("Widget"))) {
                                addHook(new FieldHook("outdatedWidgets", outdated.fin()));
                                lock.set(true);
                            }
                        }
                    }
                }
            });
        }
    }

    private class Resizable extends BlockVisitor {

        @Override
        public boolean validate() {
            return !lock.get();
        }

        @Override
        public void visit(Block block) {
            if (block.count(new MemberQuery(PUTSTATIC, "Z")) == 1 && block.target != null &&
                    block.target.count(new NumberQuery(BIPUSH, 25)) > 0) {
                FieldInsnNode fin = (FieldInsnNode) block.get(new MemberQuery(PUTSTATIC, "Z"));
                addHook(new FieldHook("resizable", fin));
            }
        }
    }
}
