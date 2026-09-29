package cn.mcmod.neotreeores.tree;

import cn.mcmod.neotreeores.NeoTreeOres;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Literal declarations must be read before block registration; CT later replays them.
 * This is intentionally not a general ZenScript interpreter: only direct top-level
 * mods.neotreeores calls with literal arguments are accepted by the pre-scan.
 */
public final class CtScriptScanner {
    private static final Pattern ROOT = Pattern.compile("^mods\\s*\\.\\s*neotreeores\\s*\\.\\s*(addOreTree|remove|configOreTree)\\s*\\(");
    private static final Pattern CALL = Pattern.compile("\\s*\\.\\s*([A-Za-z][A-Za-z0-9_]*)\\s*\\(");
    private CtScriptScanner() {}

    public static int scan(File gameDir) {
        if (gameDir == null) return 0;
        File scripts = new File(gameDir, "scripts");
        if (!scripts.isDirectory()) return 0;
        List<File> files = new ArrayList<>();
        collect(scripts, files);
        files.sort(Comparator.comparing(File::getAbsolutePath));
        int count = 0;
        for (File file : files) {
            try {
                count += scanText(file.getName(), new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            } catch (IOException e) {
                NeoTreeOres.LOGGER.error("[NeoTreeOres] pre-scan cannot read {}", file, e);
            }
        }
        NeoTreeOres.LOGGER.info("[NeoTreeOres] pre-scan: {} accepted declarations (source order)", count);
        return count;
    }

    private static void collect(File dir, List<File> files) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) collect(child, files);
            else if (child.getName().toLowerCase(Locale.ROOT).endsWith(".zs")) files.add(child);
        }
    }

    /** Also used by the headless regression harness with the user's exact script. */
    public static int scanText(String fileName, String text) {
        int count = 0;
        for (String statement : split(stripComments(text), ';')) {
            String s = statement.trim();
            Matcher root = ROOT.matcher(s);
            if (!root.find()) continue;
            try {
                int open = root.end() - 1;
                int close = closeParen(s, open);
                List<String> args = split(s.substring(open + 1, close), ',');
                String operation = root.group(1);
                if ("addOreTree".equals(operation) && args.size() > 1) {
                    if (!s.substring(close + 1).trim().isEmpty() || args.size() < 5 || args.size() > 7)
                        throw new IllegalArgumentException("invalid positional addOreTree");
                    if (CtTrees.addFromPreScan(string(args.get(0)), integer(args.get(1)), integer(args.get(2)),
                            string(args.get(3)), integer(args.get(4)), args.size() > 5 ? integer(args.get(5)) : 1,
                            args.size() > 6 ? integer(args.get(6)) : 2)) count++;
                    continue;
                }
                if (args.size() != 1) throw new IllegalArgumentException("expected one literal OD name");
                String od = string(args.get(0));
                if ("remove".equals(operation)) {
                    if (!s.substring(close + 1).trim().isEmpty()) throw new IllegalArgumentException("unexpected remove suffix");
                    if (CtTrees.remove(od)) count++;
                    continue;
                }
                boolean config = "configOreTree".equals(operation);
                String shape = "OAK";
                int log = CtOreTree.DEFAULT_LOG_COLOR, leaf = CtOreTree.DEFAULT_FOLIAGE_COLOR;
                int level = CtOreTree.DEFAULT_TOOL_LEVEL, hardness = (int) CtOreTree.DEFAULT_HARDNESS;
                Integer required = null, yield = null;
                int[] dimensions = null;
                Boolean blacklist = null;
                boolean terminal = false;
                int pos = close + 1;
                while (!s.substring(pos).trim().isEmpty()) {
                    Matcher call = CALL.matcher(s);
                    call.region(pos, s.length());
                    if (!call.lookingAt() || terminal) throw new IllegalArgumentException("invalid builder suffix");
                    String method = call.group(1);
                    open = call.end() - 1;
                    close = closeParen(s, open);
                    args = split(s.substring(open + 1, close), ',');
                    pos = close + 1;
                    if ("build".equals(method) || "configure".equals(method)) {
                        if (!args.isEmpty() || config != "configure".equals(method))
                            throw new IllegalArgumentException("wrong builder terminal");
                        terminal = true;
                        continue;
                    }
                    if ("setDimensionRequirement".equals(method)) {
                        if (args.size() != 2) throw new IllegalArgumentException("expected int[] and boolean");
                        dimensions = array(args.get(0));
                        blacklist = bool(args.get(1));
                        continue;
                    }
                    if (args.size() != 1) throw new IllegalArgumentException("expected one setter argument");
                    String value = args.get(0);
                    switch (method) {
                        case "setRecipeRequired": required = integer(value); break;
                        case "setAmountTransfered": yield = integer(value); break;
                        case "setTreeType": if (config) throw new IllegalArgumentException("config cannot change tree type"); shape = string(value); break;
                        case "setTreeColor": if (config) throw new IllegalArgumentException("config cannot change tree color"); log = integer(value); break;
                        case "setLeafColor": if (config) throw new IllegalArgumentException("config cannot change leaf color"); leaf = integer(value); break;
                        case "setMiningLevel": if (config) throw new IllegalArgumentException("config cannot change mining level"); level = integer(value); break;
                        case "setHardness": if (config) throw new IllegalArgumentException("config cannot change hardness"); hardness = integer(value); break;
                        default: throw new IllegalArgumentException("unknown setter " + method);
                    }
                }
                // configOreTree setters may end with ';', matching the original requested syntax.
                if (!config && !terminal) throw new IllegalArgumentException("addOreTree requires build()");
                boolean ok = config
                        ? CtTrees.configurePatch(od, required, yield, dimensions, blacklist)
                        : CtTrees.addFromPreScan(od, log, leaf, shape, required == null ? 0 : required,
                                yield == null ? 1 : yield, level, hardness,
                                dimensions == null ? new int[0] : dimensions, blacklist != null && blacklist);
                if (ok) count++;
            } catch (IllegalArgumentException e) {
                NeoTreeOres.LOGGER.error("[NeoTreeOres] {}: rejected declaration: {}", fileName, e.getMessage());
            }
        }
        return count;
    }

    private static int closeParen(String s, int open) {
        int depth = 0;
        char quote = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == '\\') i++;
                else if (c == quote) quote = 0;
            } else if (c == '\'' || c == '"') quote = c;
            else if (c == '(') depth++;
            else if (c == ')' && --depth == 0) return i;
        }
        throw new IllegalArgumentException("unclosed call");
    }

    /** Strip comments without consuming delimiters inside string literals. */
    private static String stripComments(String s) {
        StringBuilder out = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                out.append(c);
                if (c == '\\' && i + 1 < s.length()) out.append(s.charAt(++i));
                else if (c == quote) quote = 0;
            } else if (c == '\'' || c == '"') { quote = c; out.append(c); }
            else if (c == '#' || (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '/')) {
                while (i < s.length() && s.charAt(i) != '\n') i++;
                out.append('\n');
            } else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < s.length() && !(s.charAt(i) == '*' && s.charAt(i + 1) == '/')) {
                    if (s.charAt(i) == '\n') out.append('\n');
                    i++;
                }
                i++;
                out.append(' ');
            } else out.append(c == '\uFEFF' ? ' ' : c);
        }
        return out.toString();
    }

    private static List<String> split(String s, char separator) {
        List<String> result = new ArrayList<>();
        int depth = 0, start = 0;
        char quote = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == '\\') i++;
                else if (c == quote) quote = 0;
            } else if (c == '\'' || c == '"') quote = c;
            else if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') depth--;
            else if (c == separator && depth == 0) {
                result.add(s.substring(start, i).trim()); start = i + 1;
            }
        }
        if (!s.substring(start).trim().isEmpty()) result.add(s.substring(start).trim());
        return result;
    }

    private static String string(String s) {
        s = s.trim();
        if (s.length() < 2 || (s.charAt(0) != '"' && s.charAt(0) != '\'') || s.charAt(s.length()-1) != s.charAt(0))
            throw new IllegalArgumentException("expected literal string: " + s);
        return s.substring(1, s.length()-1).replace("\\\"", "\"").replace("\\'", "'").replace("\\\\", "\\");
    }

    private static int integer(String s) {
        s = s.trim().replace("_", "");
        try {
            if (s.startsWith("0x") || s.startsWith("0X")) {
                long n = Long.parseLong(s.substring(2), 16);
                if (n < 0 || n > 0xFFFFFFFFL) throw new NumberFormatException();
                return (int)n;
            }
            return Integer.parseInt(s);
        } catch (NumberFormatException e) { throw new IllegalArgumentException("expected integer literal: " + s); }
    }

    private static boolean bool(String s) {
        if ("true".equals(s.trim())) return true;
        if ("false".equals(s.trim())) return false;
        throw new IllegalArgumentException("expected boolean literal: " + s);
    }

    private static int[] array(String s) {
        s = s.trim();
        if (!s.startsWith("[") || !s.endsWith("]")) throw new IllegalArgumentException("expected int[] literal: " + s);
        List<String> values = split(s.substring(1, s.length()-1), ',');
        int[] result = new int[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = integer(values.get(i));
        return result;
    }
}
