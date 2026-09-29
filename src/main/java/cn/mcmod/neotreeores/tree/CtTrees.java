package cn.mcmod.neotreeores.tree;

import cn.mcmod.neotreeores.NeoTreeOres;
import cn.mcmod.neotreeores.integration.crafttweaker.CtOreTreeBuilder;

import java.util.*;

/** Pre-registration declarations and OD-keyed overrides, shared by built-in and script trees. */
public final class CtTrees {
    private static final Map<String, CtOreTree> TREES = new LinkedHashMap<>();
    private static final Set<String> REMOVED = new HashSet<>();
    private static final Map<String, Settings> SETTINGS = new HashMap<>();
    private static final Map<String, List<Settings>> CONFIG_HISTORY = new HashMap<>();
    private static boolean frozen;

    private static final class Settings {
        Integer required;
        Integer yield;
        int[] dimensions;
        boolean blacklist;
    }

    private CtTrees() {}

    private static String key(String od) {
        return od == null ? "" : od.trim().toLowerCase(Locale.ROOT);
    }

    /** Must run before Register<Block>: later CT execution validates/replays, never unregisters blocks. */
    public static void freeze() { frozen = true; }

    public static IOreTree findByOre(String od) {
        String name = key(od);
        for (OreTreeType tree : OreTreeType.values()) {
            if (key(tree.getOdName()).equals(name)) return tree;
        }
        for (CtOreTree tree : TREES.values()) {
            if (key(tree.getOdName()).equals(name)) return tree;
        }
        return null;
    }

    public static boolean isRemoved(IOreTree tree) {
        return tree != null && REMOVED.contains(key(tree.getOdName()));
    }

    public static boolean addFromPreScan(String od, int log, int leaf, String shape,
                                         int required, int level, float hardness) {
        return addInternal(od, log, leaf, shape, required, 1, level, hardness, new int[0], false);
    }

    public static boolean addFromPreScan(String od, int log, int leaf, String shape,
                                         int required, int yield, int level, int hardness,
                                         int[] dimensions, boolean blacklist) {
        return addInternal(od, log, leaf, shape, required, yield, level, hardness, dimensions, blacklist);
    }

    public static boolean add(String od, int log, int leaf, String shape,
                              int required, int level, float hardness) {
        return addInternal(od, log, leaf, shape, required, 1, level, hardness, new int[0], false);
    }

    public static boolean add(String od, int log, int leaf, String shape,
                              int required, int yield, int level, int hardness,
                              int[] dimensions, boolean blacklist) {
        return addInternal(od, log, leaf, shape, required, yield, level, hardness, dimensions, blacklist);
    }

    private static boolean addInternal(String od, int log, int leaf, String shapeName,
                                       int required, int yield, int level, float hardness,
                                       int[] dimensions, boolean blacklist) {
        String id = CtOreTree.normalizeId(od);
        OreTreeType.Shape shape = CtOreTree.parseShape(shapeName);
        if (id == null || shape == null || hardness <= 0 || level < 0 || yield < 0 || yield > 64) {
            NeoTreeOres.LOGGER.error("[NeoTreeOres] addOreTree({}): invalid declaration", od);
            return false;
        }
        if (OreTreeType.byId(id) != null) {
            NeoTreeOres.LOGGER.error("[NeoTreeOres] addOreTree({}): built-in tree already exists; use configOreTree", od);
            return false;
        }
        CtOreTree existing = TREES.get(id);
        if (existing != null) {
            boolean same = existing.sameAs(od, log, leaf, shape, required, level, hardness)
                    && existing.getOreYield() == (required > 0 ? yield : 0)
                    && Arrays.equals(existing.getDimensions(), dimensions == null ? new int[0] : dimensions)
                    && existing.isDimensionBlacklist() == blacklist;
            if (!same) NeoTreeOres.LOGGER.error("[NeoTreeOres] addOreTree({}): differs from pre-scan; restart with literal declarations", od);
            return same;
        }
        if (frozen) {
            NeoTreeOres.LOGGER.error("[NeoTreeOres] addOreTree({}): block registration is closed; use literal declarations and restart", od);
            return false;
        }
        TREES.put(id, new CtOreTree(od.trim(), log, leaf, shape, required, yield, level, hardness, dimensions, blacklist));
        NeoTreeOres.LOGGER.info("[NeoTreeOres] addOreTree: {} (id={}, shape={})", od, id, shape);
        return true;
    }

    public static boolean remove(String od) {
        IOreTree tree = findByOre(od);
        if (tree == null) {
            NeoTreeOres.LOGGER.warn("[NeoTreeOres] remove({}): unknown OD name", od);
            return false;
        }
        if (isRemoved(tree)) return true; // CT replay of a pre-scanned removal.
        if (frozen) {
            NeoTreeOres.LOGGER.error("[NeoTreeOres] remove({}): too late to unregister; put a literal remove call in the script and restart", od);
            return false;
        }
        REMOVED.add(key(tree.getOdName()));
        NeoTreeOres.LOGGER.info("[NeoTreeOres] remove({}): excluded before block/item/recipe registration", tree.getOdName());
        return true;
    }

    public static boolean configure(String od, int required, int yield, int[] dimensions, boolean blacklist) {
        return configurePatch(od, required, yield, dimensions == null ? new int[0] : dimensions, blacklist);
    }

    /** Null fields are deliberately omitted, not reset to builder defaults. */
    public static boolean configurePatch(String od, Integer required, Integer yield, int[] dimensions, Boolean blacklist) {
        IOreTree tree = findByOre(od);
        // CT replays the source, including configurations superseded or removed later.
        if (frozen && tree != null && matchesPreScan(od, required, yield, dimensions, blacklist)) return true;
        if (tree == null || isRemoved(tree)) {
            NeoTreeOres.LOGGER.warn("[NeoTreeOres] configOreTree({}): tree does not exist or was removed", od);
            return false;
        }
        if (!validAmounts(required, yield) || ((dimensions == null) != (blacklist == null))) {
            NeoTreeOres.LOGGER.error("[NeoTreeOres] configOreTree({}): required must be 0..9, output 0..64; dimension list and flag form a pair", od);
            return false;
        }
        if (frozen) {
            boolean same = (required == null || required == recipeRequired(tree))
                    && (yield == null || yield == recipeYield(tree))
                    && (dimensions == null || (Arrays.equals(dimensions, dimensions(tree))
                    && blacklist == blacklist(tree)));
            if (!same) NeoTreeOres.LOGGER.error("[NeoTreeOres] configOreTree({}): late change differs from pre-scan; use literal declarations and restart", od);
            return same;
        }
        Settings record = new Settings();
        record.required = required;
        record.yield = yield;
        record.dimensions = dimensions == null ? null : dimensions.clone();
        record.blacklist = blacklist != null && blacklist;
        CONFIG_HISTORY.computeIfAbsent(key(tree.getOdName()), unused -> new ArrayList<>()).add(record);
        Settings settings = SETTINGS.computeIfAbsent(key(tree.getOdName()), unused -> new Settings());
        if (required != null) settings.required = required;
        if (yield != null) settings.yield = yield;
        if (dimensions != null) {
            settings.dimensions = dimensions.clone();
            settings.blacklist = blacklist;
        }
        NeoTreeOres.LOGGER.info("[NeoTreeOres] configOreTree({}): required={}, output={}, dimensions={}, blacklist={}",
                tree.getOdName(), recipeRequired(tree), recipeYield(tree), Arrays.toString(dimensions(tree)), blacklist(tree));
        return true;
    }

    private static boolean matchesPreScan(String od, Integer required, Integer yield, int[] dimensions, Boolean blacklist) {
        List<Settings> history = CONFIG_HISTORY.get(key(od));
        if (history == null || ((dimensions == null) != (blacklist == null))) return false;
        for (Settings s : history) {
            if ((required == null || required.equals(s.required))
                    && (yield == null || yield.equals(s.yield))
                    && (dimensions == null || Arrays.equals(dimensions, s.dimensions) && blacklist == s.blacklist)) return true;
        }
        return false;
    }

    private static boolean validAmounts(Integer required, Integer yield) {
        return (required == null || required >= 0 && required <= 9)
                && (yield == null || yield >= 0 && yield <= 64);
    }

    public static int recipeRequired(IOreTree tree) {
        Settings s = SETTINGS.get(key(tree.getOdName()));
        return s != null && s.required != null ? s.required : tree.getOreFromLeavesCount();
    }

    public static int recipeYield(IOreTree tree) {
        Settings s = SETTINGS.get(key(tree.getOdName()));
        return s != null && s.yield != null ? s.yield : tree.getOreYield();
    }

    private static int[] dimensions(IOreTree tree) {
        Settings s = SETTINGS.get(key(tree.getOdName()));
        if (s != null && s.dimensions != null) return s.dimensions;
        return tree instanceof CtOreTree ? ((CtOreTree) tree).getDimensions() : new int[0];
    }

    private static boolean blacklist(IOreTree tree) {
        Settings s = SETTINGS.get(key(tree.getOdName()));
        if (s != null && s.dimensions != null) return s.blacklist;
        return tree instanceof CtOreTree && ((CtOreTree) tree).isDimensionBlacklist();
    }

    public static boolean allowsDimension(IOreTree tree, int dimension) {
        if (tree == null || isRemoved(tree)) return false;
        int[] dims = dimensions(tree);
        if (dims.length == 0) return true; // Preserve the documented default: no restriction.
        boolean listed = false;
        for (int id : dims) if (id == dimension) { listed = true; break; }
        return blacklist(tree) ? !listed : listed;
    }

    public static CtOreTreeBuilder config(String od) { return new CtOreTreeBuilder(od, true); }

    public static List<CtOreTree> all() {
        List<CtOreTree> active = new ArrayList<>();
        for (CtOreTree tree : TREES.values()) if (!isRemoved(tree)) active.add(tree);
        return Collections.unmodifiableList(active);
    }
    public static boolean isEmpty() { return all().isEmpty(); }
    public static int size() { return all().size(); }
}
