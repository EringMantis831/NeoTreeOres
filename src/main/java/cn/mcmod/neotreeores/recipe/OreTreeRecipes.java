package cn.mcmod.neotreeores.recipe;

import cn.mcmod.neotreeores.NeoTreeOres;
import cn.mcmod.neotreeores.tree.IOreTree;
import cn.mcmod.neotreeores.tree.CtTrees;
import cn.mcmod.neotreeores.tree.OreTreeContent;
import cn.mcmod.neotreeores.tree.OreTreeRegistry;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.List;
import java.util.Locale;

/**
 * 矿石树的默认合成配方（内置 19 棵 + 脚本新增树统一走这里）。
 *
 * <p><b>① 落叶 → 矿物（无序合成）</b>：消耗量与产出量来自 {@code task.xls}
 * （内置树写死在 {@code OreTreeType}；脚本树用 {@code amountRequiredtoOre}，产出 1 个）：</p>
 * <pre>
 *   煤炭树   3 落叶 → 1 煤炭        电木     6 落叶 → 16 红石
 *   铁树     5 落叶 → 1 铁锭        蓝琉璃木 7 落叶 → 12 青金石
 *   金丝木   6 落叶 → 1 金锭        晶化木   9 落叶 → 1 钻石 …
 * </pre>
 * <p>矿物按矿词查找，优先级：{@code ingot<OD>} → {@code gem<OD>} → {@code dust<OD>} →
 * {@code ore<OD>} → {@code <OD>} → 小写 {@code <od>}（照顾原版煤炭这种小写矿词）。</p>
 *
 * <p><b>② 树苗（有序合成）</b>：8 个对应矿物块围一圈 + 中间任意树苗 → 该树的树苗：</p>
 * <pre>
 *   BBB      B = block&lt;OD&gt;（如 blockIron）
 *   BSB      S = treeSapling（任意树苗）
 *   BBB      产物 = 该树树苗 × 1
 * </pre>
 *
 * <p>找不到对应矿词/矿物块时只是跳过该条配方并写日志（检测型树在没装对应矿物 mod 时很正常）。</p>
 */
@Mod.EventBusSubscriber(modid = NeoTreeOres.MODID)
public final class OreTreeRecipes {

    /** 工作台 3×3 的格子上限 */
    private static final int MAX_REQUIRED = 9;

    /**
     * 矿物矿词检索优先级：先"物品类"（锭/宝石/粉/粒），再裸名（含小写，照顾原版 coal），
     * 最后才是 {@code ore<OD>}（那是"矿石方块"，只在前面的都找不到时兜底，
     * 否则煤炭树会拿煤矿石当产物）。
     */
    private static final String[] ORE_PREFIXES = {"ingot", "gem", "dust", "nugget", ""};
    private static final String ORE_LAST_RESORT = "ore";

    private OreTreeRecipes() {
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        int leafRecipes = 0;
        int saplingRecipes = 0;
        int woodRecipes = 0;

        for (IOreTree tree : OreTreeRegistry.allTrees()) {
            OreTreeContent content = OreTreeRegistry.get(tree);
            if (content != null && content.getLog() != null && content.getWood() != null) {
                // Exact tree-specific block, wildcard orientation metadata; never use logWood.
                ShapedOreRecipe wood = new ShapedOreRecipe(null, new ItemStack(content.getWood(), 3),
                        "LL", "LL", 'L', new ItemStack(content.getLog(), 1,
                        net.minecraftforge.oredict.OreDictionary.WILDCARD_VALUE));
                wood.setRegistryName(new ResourceLocation(NeoTreeOres.MODID, tree.getId() + "_log_to_wood"));
                event.getRegistry().register(wood);
                woodRecipes++;
            }
            if (content == null || content.getLeafDrop() == null || content.getSapling() == null) {
                continue;
            }
            if (registerLeafToOre(event, tree, content)) {
                leafRecipes++;
            }
            if (registerSapling(event, tree, content)) {
                saplingRecipes++;
            }
        }

        NeoTreeOres.LOGGER.info("[NeoTreeOres] recipes: leaves->ore={}, sapling={}, log->wood={} (4:3, one-way)",
                Integer.valueOf(leafRecipes), Integer.valueOf(saplingRecipes), Integer.valueOf(woodRecipes));
    }

    // ------------------------------------------------------------------
    // ① 落叶 → 矿物（无序）
    // ------------------------------------------------------------------

    private static boolean registerLeafToOre(RegistryEvent.Register<IRecipe> event, IOreTree tree,
                                             OreTreeContent content) {
        int need = CtTrees.recipeRequired(tree);
        int yield = CtTrees.recipeYield(tree);
        if (need <= 0 || yield <= 0) {
            return false;   // 该树不提供转化配方（例如霓虹树 / 脚本里写 <=0）
        }

        ItemStack output = findOre(tree.getOdName(), yield);
        if (output.isEmpty()) {
            // 检测型树没装对应矿物 mod 时属正常，只提示不告警
            if (tree.isAlwaysEnabled()) {
                NeoTreeOres.LOGGER.warn("[NeoTreeOres] {}: 找不到矿词 {} 对应的矿物，跳过落叶转化配方",
                        tree.getId(), describeCandidates(tree.getOdName()));
            } else {
                NeoTreeOres.LOGGER.info("[NeoTreeOres] {}: 未检测到矿词 {}，跳过落叶转化配方",
                        tree.getId(), describeCandidates(tree.getOdName()));
            }
            return false;
        }

        int count = need;
        if (count > MAX_REQUIRED) {
            NeoTreeOres.LOGGER.warn("[NeoTreeOres] {} 的落叶消耗量 {} 超过工作台上限，已按 {} 注册配方",
                    new Object[] {tree.getId(), Integer.valueOf(need), Integer.valueOf(MAX_REQUIRED)});
            count = MAX_REQUIRED;
        }

        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (int i = 0; i < count; i++) {
            ingredients.add(Ingredient.fromItem(content.getLeafDrop()));
        }
        ShapelessRecipes recipe = new DimensionLeavesRecipe(tree, output, ingredients);
        recipe.setRegistryName(new ResourceLocation(NeoTreeOres.MODID, tree.getId() + "_leaves_to_ore"));
        event.getRegistry().register(recipe);
        NeoTreeOres.LOGGER.info("[NeoTreeOres] recipe: {} x {} -> {} x {} (oreDict={})",
                new Object[] {Integer.valueOf(count), content.getLeafDrop().getRegistryName(),
                        Integer.valueOf(yield), output.getDisplayName(), foundOreName(tree.getOdName())});
        return true;
    }

    /** 按优先级找一个矿词，返回数量为 count 的副本；都找不到返回空 */
    private static ItemStack findOre(String odName, int count) {
        for (String prefix : ORE_PREFIXES) {
            ItemStack stack = firstOre(prefix + odName, count);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        // 原版 coal / charcoal 这类是小写裸名
        ItemStack lower = firstOre(odName.toLowerCase(Locale.ROOT), count);
        if (!lower.isEmpty()) {
            return lower;
        }
        // 再兜底：原版 1.12.2 压根没有对应矿词的矿物（目前只有煤炭/木炭）
        ItemStack gap = vanillaGap(odName.toLowerCase(Locale.ROOT), count);
        if (!gap.isEmpty()) {
            return gap;
        }
        // 最后兜底：矿石方块（ore<OD>）
        return firstOre(ORE_LAST_RESORT + odName, count);
    }

    /**
     * 原版矿词缺口：1.12.2 只给煤炭注册了 {@code oreCoal}/{@code blockCoal}，
     * 没有物品级的矿词，所以这里显式补上（不做通用的"按注册名查物品"，
     * 否则 OD 名 Lead 会匹配到原版的拴绳 minecraft:lead）。
     */
    private static ItemStack vanillaGap(String odLower, int count) {
        if ("coal".equals(odLower)) {
            return new ItemStack(Items.COAL, count, 0);
        }
        if ("charcoal".equals(odLower)) {
            return new ItemStack(Items.COAL, count, 1);
        }
        return ItemStack.EMPTY;
    }

    /** 找出命中的矿词名（只用于日志） */
    private static String foundOreName(String odName) {
        for (String prefix : ORE_PREFIXES) {
            if (!OreDictionary.getOres(prefix + odName, false).isEmpty()) {
                return prefix + odName;
            }
        }
        if (!OreDictionary.getOres(odName.toLowerCase(Locale.ROOT), false).isEmpty()) {
            return odName.toLowerCase(Locale.ROOT);
        }
        if (!vanillaGap(odName.toLowerCase(Locale.ROOT), 1).isEmpty()) {
            return "vanilla:" + odName.toLowerCase(Locale.ROOT);
        }
        if (!OreDictionary.getOres(ORE_LAST_RESORT + odName, false).isEmpty()) {
            return ORE_LAST_RESORT + odName;
        }
        return null;
    }

    /** Called after every mod has registered its items and ore dictionary entries. */
    public static int registerLogSmelting() {
        int registered = 0;
        for (IOreTree tree : OreTreeRegistry.allTrees()) {
            OreTreeContent content = OreTreeRegistry.get(tree);
            ItemStack output = firstLogNugget(tree, content);
            if (output.isEmpty()) {
                continue;
            }
            for (int metadata : new int[] {0, 4, 8, 12}) {
                FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(content.getLog(), 1, metadata),
                        output.copy(), 0.2F);
            }
            registered++;
        }
        NeoTreeOres.LOGGER.info("[NeoTreeOres] log smelting: {} tree logs registered (1 nugget, 0.2 XP)",
                Integer.valueOf(registered));
        return registered;
    }

    public static ItemStack firstLogNugget(IOreTree tree, OreTreeContent content) {
        if (content == null || content.getLog() == null || content.getLog().isAllBark()) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> nuggets = OreDictionary.getOres("nugget" + tree.getOdName(), false);
        if (nuggets.isEmpty() || nuggets.get(0).isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack output = nuggets.get(0).copy();
        output.setCount(1);
        return output;
    }

    private static ItemStack firstOre(String oreName, int count) {
        List<ItemStack> ores = OreDictionary.getOres(oreName, false);
        if (ores.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = ores.get(0).copy();
        stack.setCount(count);
        return stack;
    }

    private static String describeCandidates(String odName) {
        StringBuilder sb = new StringBuilder();
        for (String prefix : ORE_PREFIXES) {
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append(prefix).append(odName);
        }
        sb.append('/').append(odName.toLowerCase(Locale.ROOT));
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // ② 树苗（有序：8 个矿物块 + 任意树苗）
    // ------------------------------------------------------------------

    private static boolean registerSapling(RegistryEvent.Register<IRecipe> event, IOreTree tree,
                                           OreTreeContent content) {
        String blockOre = "block" + tree.getOdName();
        if (OreDictionary.getOres(blockOre, false).isEmpty()) {
            return false;   // 没有对应矿物块（检测型树常见）
        }
        if (OreDictionary.getOres("treeSapling", false).isEmpty()) {
            return false;   // 连"任意树苗"矿词都没有，配方没法成立
        }
        ShapedOreRecipe recipe = new ShapedOreRecipe(null, new ItemStack(content.getSapling()),
                "BBB", "BSB", "BBB",
                'B', blockOre,
                'S', "treeSapling");
        recipe.setRegistryName(new ResourceLocation(NeoTreeOres.MODID, tree.getId() + "_sapling"));
        event.getRegistry().register(recipe);
        NeoTreeOres.LOGGER.debug("[NeoTreeOres] 树苗配方: 8 x {} + 任意树苗 -> {}",
                blockOre, content.getSapling().getRegistryName());
        return true;
    }
}
