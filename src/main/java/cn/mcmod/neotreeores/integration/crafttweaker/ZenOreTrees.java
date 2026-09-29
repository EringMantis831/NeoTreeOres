package cn.mcmod.neotreeores.integration.crafttweaker;

import cn.mcmod.neotreeores.tree.CtTrees;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * CraftTweaker（ZenScript）联动入口 —— 整合包作者加树只需要一行脚本：
 *
 * <pre>
 * mods.neotreeores.addOreTree("Copper", 0xD07A3F, 0xD98A4A, "OAK", 4);
 * mods.neotreeores.addOreTree("Tin",    0xB9C4C8, 0xC2CCD0, "SPRUCE", 4, 1, 2);
 * </pre>
 *
 * <p>参数含义：</p>
 * <ul>
 *   <li>{@code OreDictName} —— 对应的矿物 OD 名（例如 {@code "Copper"}，会按 {@code ingotCopper}
 *       等前缀自动探测该矿物是否存在来决定这棵树是否出现在创造栏）；</li>
 *   <li>{@code LogColor} —— 树干方块颜色（RRGGBB，作用于原木与木头方块）；</li>
 *   <li>{@code LeafColor} —— 树叶颜色，同时影响树叶方块与落叶物品；</li>
 *   <li>{@code TreeType} —— 只能是 {@code "OAK"} 或 {@code "SPRUCE"}；</li>
 *   <li>{@code amountRequiredtoOre} —— 合成 1 个对应矿物锭需要几个落叶物品，会自动加配方；
 *       {@code <= 0} 表示不加配方；</li>
 *   <li>{@code ToolLevel} —— 原木方块的挖掘等级，默认 1；</li>
 *   <li>{@code Hardness} —— 原木方块的硬度，默认 2。</li>
 * </ul>
 *
 * <p><b>软依赖</b>：没装 CraftTweaker 时本类不会被加载，mod 依旧只有内置 19 棵树。
 * CT 在 preInit 阶段执行脚本，因此这里只是"登记声明"，真正的方块/物品注册仍由
 * {@code OreTreeRegistry} 在 RegistryEvent 里完成。</p>
 *
 * <p>注意：方块/物品一旦注册无法撤销，所以<b>改脚本后必须重启游戏</b>。</p>
 */
@ZenClass("mods.neotreeores")
@ZenRegister
public final class ZenOreTrees {

    private ZenOreTrees() {
    }

    /**
     * 新增一棵矿石树。
     *
     * @param oreDictName        对应矿物 OD 名（决定方块 id 与矿词，例如 Copper → copper_log / saplingCopper）
     * @param logColor           树干颜色 RRGGBB（原木 + 木头方块）
     * @param leafColor          树叶/落叶物品颜色 RRGGBB
     * @param treeType           树形："OAK" 或 "SPRUCE"
     * @param amountRequiredtoOre 合成 1 个对应矿物锭需要的落叶物品数（<=0 不加配方）
     * @param toolLevel          原木方块挖掘等级（默认 1）
     * @param hardness           原木方块硬度（默认 2）
     */
    @ZenMethod
    public static CtOreTreeBuilder addOreTree(String oreDictName) {
        return new CtOreTreeBuilder(oreDictName);
    }

    /** Legacy positional form kept for existing scripts. */
    @ZenMethod
    public static void addOreTree(String oreDictName, int logColor, int leafColor, String treeType,
                                  int amountRequiredToOre,
                                  @Optional(valueLong = 1) int toolLevel,
                                  @Optional(valueLong = 2) int hardness) {
        new CtOreTreeBuilder(oreDictName)
                .setTreeColor(logColor)
                .setLeafColor(leafColor)
                .setTreeType(treeType)
                .setRecipeRequired(amountRequiredToOre)
                .setMiningLevel(toolLevel)
                .setHardness(hardness)
                .build();
    }

    @ZenMethod
    public static void remove(String oreDictName) {
        CtTrees.remove(oreDictName);
    }

    @ZenMethod
    public static CtOreTreeBuilder configOreTree(String oreDictName) {
        return CtTrees.config(oreDictName);
    }
}
