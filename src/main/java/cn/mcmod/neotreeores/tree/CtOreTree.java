package cn.mcmod.neotreeores.tree;

import java.util.Locale;

/**
 * 一棵"脚本新增树" —— 由 CraftTweaker 的 ZenScript 在 preInit 阶段声明：
 *
 * <pre>
 * mods.neotreeores.addOreTree("Copper", 0xD07A3F, 0xD98A4A, "OAK", 4, 1, 2);
 * </pre>
 *
 * <p>与内置 19 棵树走完全相同的注册/门控/掉落规则（见 {@link IOreTree}）。
 * 差别只有两点：</p>
 * <ul>
 *   <li>它不可能随包发布专属材质 → 方块贴图一律在客户端运行期由"灰度基底 + 颜色"生成；</li>
 *   <li>它带有脚本给的额外参数：合成配比、原木挖掘等级与硬度。</li>
 * </ul>
 */
public final class CtOreTree implements IOreTree {

    /** 没写颜色时的兜底 */
    public static final int DEFAULT_LOG_COLOR = 0x8A8A8A;
    public static final int DEFAULT_FOLIAGE_COLOR = 0x7FBF5F;

    /** 默认挖掘等级 / 硬度（脚本未给 @Optional 参数时） */
    public static final int DEFAULT_TOOL_LEVEL = 1;
    public static final float DEFAULT_HARDNESS = 2.0F;

    private final String oreDictName;      // 脚本给的原始 OD 名，如 "Copper"
    private final String id;               // 规范化后的小写 id，如 "copper"
    private final OreTreeType.Shape shape;
    private final int logColor;
    private final int foliageColor;
    private int amountRequiredToOre;
    private int amountTransferredToOre;
    private int toolLevel;
    private int[] dimensionList;
    private boolean dimensionBlacklist;
    private final float hardness;
    private final String displayName;

    public CtOreTree(String oreDictName, int logColor, int foliageColor, OreTreeType.Shape shape,
                     int amountRequiredToOre, int amountTransferredToOre, int toolLevel, float hardness,
                     int[] dimensionList, boolean dimensionBlacklist) {
        this.oreDictName = oreDictName;
        this.id = normalizeId(oreDictName);
        this.shape = shape;
        this.logColor = logColor & 0xFFFFFF;
        this.foliageColor = foliageColor & 0xFFFFFF;
        this.amountRequiredToOre = amountRequiredToOre;
        this.amountTransferredToOre = Math.max(0, amountTransferredToOre);
        this.toolLevel = toolLevel;
        this.dimensionList = dimensionList == null ? new int[0] : dimensionList.clone();
        this.dimensionBlacklist = dimensionBlacklist;
        this.hardness = hardness;
        this.displayName = oreDictName;
    }

    /** 规范化 id：小写、只保留 [a-z0-9_]；非法（空 / 数字开头）返回 null */
    public static String normalizeId(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        s = s.replaceAll("_+", "_").replaceAll("^_+|_+$", "");
        if (s.isEmpty() || !Character.isLetter(s.charAt(0))) {
            return null;
        }
        return s;
    }

    /** 解析脚本给的树形：OAK / SPRUCE（大小写不敏感，也认 broadleaf / needleleaf） */
    public static OreTreeType.Shape parseShape(String raw) {
        if (raw == null) {
            return null;
        }
        switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "oak":
            case "broadleaf":
                return OreTreeType.Shape.OAK;
            case "spruce":
            case "needleleaf":
                return OreTreeType.Shape.SPRUCE;
            default:
                return null;
        }
    }

    // ------------------------------------------------------------------
    // IOreTree
    // ------------------------------------------------------------------

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getOdName() {
        return oreDictName;
    }

    @Override
    public String getChineseName() {
        return displayName;
    }

    @Override
    public String getEnglishName() {
        return displayName;
    }

    @Override
    public OreTreeType.Shape getShape() {
        return shape;
    }

    /** 按该矿词自动判定（{@code OreGate} 会依次试 ore/ingot/gem/... + OD 名） */
    @Override
    public OreTreeType.Gate getGate() {
        return OreTreeType.Gate.DETECT;
    }

    @Override
    public int getDefaultLogColor() {
        return logColor;
    }

    @Override
    public int getDefaultFoliageColor() {
        return foliageColor;
    }

    // ------------------------------------------------------------------
    // 脚本专属参数
    // ------------------------------------------------------------------

    /** 合成 1 个对应矿物锭需要几个落叶物品；小于等于 0 表示不加配方 */
    public int getAmountRequiredToOre() {
        return amountRequiredToOre;
    }

    public void configure(int required, int transferred, int[] dimensions, boolean blacklist) {
        amountRequiredToOre = required;
        amountTransferredToOre = Math.max(0, transferred);
        dimensionList = dimensions == null ? new int[0] : dimensions.clone();
        dimensionBlacklist = blacklist;
    }

    public int[] getDimensions() { return dimensionList.clone(); }

    public boolean isDimensionBlacklist() { return dimensionBlacklist; }

    public boolean canGrowInDimension(int dimension) {
        boolean listed = false;
        for (int id : dimensionList) if (id == dimension) { listed = true; break; }
        return dimensionBlacklist ? !listed : (dimensionList.length == 0 || listed);
    }

    /** 原木方块的挖掘等级（harvest level） */
    public int getToolLevel() {
        return toolLevel;
    }

    /** 原木方块的硬度 */
    public float getHardness() {
        return hardness;
    }

    /** 与另一次声明是否完全一致（预读 vs 脚本真实调用，用于幂等判断） */
    public boolean sameAs(String od, int log, int foliage, OreTreeType.Shape shape,
                          int amountRequiredToOre, int toolLevel, float hardness) {
        return this.oreDictName.equals(od)
                && this.logColor == (log & 0xFFFFFF)
                && this.foliageColor == (foliage & 0xFFFFFF)
                && this.shape == shape
                && this.amountRequiredToOre == amountRequiredToOre
                && this.toolLevel == toolLevel
                && Math.abs(this.hardness - hardness) < 0.0001F;
    }

    /** 脚本树：消耗量就是 amountRequiredtoOre，产出固定 1 个 */
    @Override
    public int getOreFromLeavesCount() {
        return amountRequiredToOre > 0 ? amountRequiredToOre : 0;
    }

    @Override
    public int getOreYield() {
        return amountRequiredToOre > 0 ? amountTransferredToOre : 0;
    }

    @Override
    public String toString() {
        return "CtOreTree{" + id + ", od=" + oreDictName + ", shape=" + shape + ", need=" + amountRequiredToOre + "}";
    }
}
