package cn.mcmod.neotreeores.tree;

/**
 * 一棵"矿石树"的抽象 —— 内置 19 棵树（{@link OreTreeType} 枚举）与脚本新增树
 * （{@link CtOreTree}）都实现它，这样注册、门控、贴图生成、模型重烘焙
 * 全都只认这一个类型，不需要任何 {@code if (内置) ... else (动态) ...} 分支。
 *
 * <p>所有注册名/矿词名/本地化 key 都由下面这几个基本属性推导，因此新增树形
 * 只需提供 id、矿词后缀、名字、树形、门控与两个颜色即可。</p>
 */
public interface IOreTree {

    // ------------------------------------------------------------------
    // 基本属性（由实现提供）
    // ------------------------------------------------------------------

    /** id 前缀，如 {@code iron}（必须是小写字母/数字/下划线） */
    String getId();

    /** 矿词后缀，如 {@code Iron}（首字母大写） */
    String getOdName();

    /** 中文树名，如“铁树” */
    String getChineseName();

    /** 英文树名，如 {@code Cycas} */
    String getEnglishName();

    OreTreeType.Shape getShape();

    OreTreeType.Gate getGate();

    /** 树干颜色（构建期 baked 用默认值；运行期 colored / 动态树用配置值） */
    int getDefaultLogColor();

    /** 树叶颜色（同上） */
    int getDefaultFoliageColor();

    // ------------------------------------------------------------------
    // 派生属性（默认实现，两类树共用）
    // ------------------------------------------------------------------

    /** 原木方块的挖掘等级（内置树没设 = 0；脚本树默认 1，见 {@link CtOreTree}） */
    default int getToolLevel() {
        return 0;
    }

    /** 原木方块的硬度（内置树 2.0，与 BlockOreLog 原行为一致） */
    default float getHardness() {
        return 2.0F;
    }

    /** 落叶转化：需要几个落叶物品（0 = 不提供合成配方） */
    default int getOreFromLeavesCount() {
        return 0;
    }

    /** 落叶转化：一次产出几个矿物（0 = 不提供合成配方） */
    default int getOreYield() {
        return 0;
    }

    default boolean isAlwaysEnabled() {
        return getGate() == OreTreeType.Gate.ALWAYS;
    }

    default String getLogId() {
        return getId() + "_log";
    }

    /** 六面树皮的装饰性"木头"方块 */
    default String getWoodId() {
        return getId() + "_wood";
    }

    default String getLeavesId() {
        return getId() + "_leaves";
    }

    default String getSaplingId() {
        return getId() + "_sapling";
    }

    /** 落叶地毯 */
    default String getFallenLeavesId() {
        return getId() + "_fallen_leaves";
    }

    /** 树叶掉落物（针叶/阔叶），按树形决定 */
    default String getLeafDropId() {
        return getId() + "_" + getShape().getDropSuffix();
    }

    default String getSaplingOreDict() {
        return "sapling" + getOdName();
    }

    default String getLeafDropOreDict() {
        return getShape().getDropOdPrefix() + getOdName();
    }

    default String getLogOreDict() {
        return "logWood";
    }

    // ------------------------------------------------------------------
    // 本地化 key
    // ------------------------------------------------------------------

    default String getLogLangKey() {
        return "tile.neotreeores." + getLogId() + ".name";
    }

    default String getWoodLangKey() {
        return "tile.neotreeores." + getWoodId() + ".name";
    }

    default String getLeavesLangKey() {
        return "tile.neotreeores." + getLeavesId() + ".name";
    }

    default String getSaplingLangKey() {
        return "tile.neotreeores." + getSaplingId() + ".name";
    }

    default String getFallenLeavesLangKey() {
        return "tile.neotreeores." + getFallenLeavesId() + ".name";
    }

    default String getLeafDropLangKey() {
        return "item.neotreeores." + getLeafDropId() + ".name";
    }
}
