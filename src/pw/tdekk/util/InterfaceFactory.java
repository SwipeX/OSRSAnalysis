package pw.tdekk.util;

import pw.tdekk.Updater;
import pw.tdekk.visitor.GraphVisitor;
import pw.tdekk.mod.hooks.FieldHook;
import pw.tdekk.mod.hooks.Hook;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Tim Dekker
 * @since 6/30/15
 */
public class InterfaceFactory {

    public static final String PREFIX = "RS";
    private static final String NEWLINE = "\n";

    public static void generateInterfaces(Updater updater, File targetFolder) {
        for (GraphVisitor visitor : updater.visitors) {
            String name = PREFIX + visitor.id();
            String packageVal = "package " + updater.getAccessorPackage().replaceAll("/", ".") + ";";
            String declare = String.format("public interface %s {", name);
            ArrayList<String> methods = new ArrayList<>();
            for (Map.Entry<String, Hook> hook : visitor.hooks.entrySet()) {
                String hookName = hook.getKey();
                Hook hookValue = hook.getValue();
                if (hookValue.getType() == 0) {
                    FieldHook fieldHook = (FieldHook) hookValue;
                    String desc = fieldHook.fieldDesc;
                    String translatedDesc = translateReturn(updater, desc);
                    String method = String.format("%s %s();", translatedDesc, methodify(hookName, desc));
                    methods.add(method);
                }
            }
            if (!targetFolder.exists())
                targetFolder.mkdirs();
            File file = new File(targetFolder, PREFIX + visitor.getClass().getSimpleName() + ".java");
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.write(packageVal + NEWLINE);
                writer.write(declare + NEWLINE);
                for (Iterator<String> iterator = methods.iterator(); iterator.hasNext(); ) {
                    String s = iterator.next();
                    writer.write(s + NEWLINE);
                }
                writer.write("}" + NEWLINE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static String translateReturn(Updater updater, String raw) {
        raw = raw.replace(";", "").replace("L", "");
        String type = "";
        String post = "";
        for (char c : raw.toCharArray()) {
            if (c == '[') {
                post += "[]";
            }
        }
        raw = raw.replaceAll("\\[", "");
        for (GraphVisitor visitor : updater.visitors) {
            if (visitor.cn.name.equals(raw)) {
                type = PREFIX + visitor.id();
            }
        }
        if (type.equals("")) {
            if (raw.contains("/")) {
                type = raw.substring(raw.lastIndexOf("/") + 1);
            } else {
                switch (raw) {
                    case "B":
                        return "byte";
                    case "S":
                        return "short";
                    case "I":
                        return "int";
                    case "F":
                        return "float";
                    case "D":
                        return "double";
                    case "L":
                        return "long";
                    default:
                        return "Object";
                }
            }
        }
        return type + post;
    }

    private static String methodify(String name, String desc) {
        String upperName = Character.toString(Character.toUpperCase(name.charAt(0)));
        if (name.length() > 1)
            upperName += name.substring(1);
        return desc.equals("Z") ? ("is" + upperName) : ("get" + upperName);
    }
}
