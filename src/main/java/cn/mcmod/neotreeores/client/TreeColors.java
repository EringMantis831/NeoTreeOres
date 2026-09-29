package cn.mcmod.neotreeores.client;

import cn.mcmod.neotreeores.NeoTreeOres;
import cn.mcmod.neotreeores.NeoTreeOresConfig;
import cn.mcmod.neotreeores.tree.IOreTree;
import cn.mcmod.neotreeores.tree.OreTreeContent;
import cn.mcmod.neotreeores.tree.OreTreeRegistry;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 颜色器（原版草方块/树叶用的那套 {@code tintindex + IBlockColor/IItemColor}）。
 *
 * <p>模型里所有 face 都写了 {@code "tintindex"}，颜色在这里按"方块 → 树 → 配置颜色"给出：</p>
 * <ul>
 *   <li>原木 / 木头：tint 0 = {@code LogColor}</li>
 *   <li>树叶 / 落叶地毯：tint 0 = {@code LeafColor}</li>
 *   <li>树苗 / 落叶地毯：tint 0 = {@code LeafColor}</li>
 *   <li>树苗：tint 0 = {@code LeafColor}（灰度基底那层）、
 *       tint ≥ 1 = <b>白色</b>（overlay 层不变色）—— 原版皮革甲就是这么写的：
 *       {@code ItemArmor.getColorFromItemStack} 对 tintIndex &gt; 0 返回 16777215</li>
 * </ul>
 *
 * <p>内置 19 棵树与脚本新增树走同一份逻辑，所以配置里改颜色、脚本里写颜色，
 * 都是**下一次渲染就生效**（不用重新缝合贴图）。</p>
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = NeoTreeOres.MODID, value = Side.CLIENT)
public final class TreeColors {

    private TreeColors() {
    }

    @SubscribeEvent
    public static void onBlockColors(ColorHandlerEvent.Block event) {
        int n = 0;
        for (IOreTree tree : OreTreeRegistry.allTrees()) {
            OreTreeContent content = OreTreeRegistry.get(tree);
            if (content == null) {
                continue;
            }
            final int logColor = NeoTreeOresConfig.of(tree).logColor & 0xFFFFFF;
            final int leafColor = NeoTreeOresConfig.of(tree).foliageColor & 0xFFFFFF;

            // 原木 / 木头：一律 LogColor
            if (content.getLog() != null && content.getWood() != null) {
                event.getBlockColors().registerBlockColorHandler(
                        (state, world, pos, tintIndex) -> logColor, content.getLog(), content.getWood());
            }
            // 树叶 / 地毯：LeafColor
            if (content.getLeaves() != null && content.getFallenLeaves() != null) {
                event.getBlockColors().registerBlockColorHandler(
                        (state, world, pos, tintIndex) -> leafColor, content.getLeaves(), content.getFallenLeaves());
            }
            // 树苗：层0（灰度基底）= LeafColor；层1（overlay 原画）= 白色 = 不变色
            if (content.getSapling() != null) {
                event.getBlockColors().registerBlockColorHandler(
                        (state, world, pos, tintIndex) -> tintIndex >= 1 ? 0xFFFFFF : leafColor,
                        content.getSapling());
            }
            n++;
        }
        if (n > 0) {
            NeoTreeOres.LOGGER.info("[NeoTreeOres] block colors: 已为 {} 棵树注册颜色器", Integer.valueOf(n));
        }
    }

    @SubscribeEvent
    public static void onItemColors(ColorHandlerEvent.Item event) {
        for (IOreTree tree : OreTreeRegistry.allTrees()) {
            OreTreeContent content = OreTreeRegistry.get(tree);
            if (content == null) {
                continue;
            }
            final int logColor = NeoTreeOresConfig.of(tree).logColor & 0xFFFFFF;
            final int leafColor = NeoTreeOresConfig.of(tree).foliageColor & 0xFFFFFF;

            IItemColor log = (stack, tintIndex) -> logColor;
            IItemColor leaf = (stack, tintIndex) -> leafColor;
            IItemColor sapling = (stack, tintIndex) -> tintIndex >= 1 ? 0xFFFFFF : leafColor;

            register(event, log, content.getLog(), content.getWood());
            register(event, leaf, content.getLeaves(), content.getFallenLeaves());
            register(event, sapling, content.getSapling());
            if (content.getLeafDrop() != null) {
                event.getItemColors().registerItemColorHandler(leaf, content.getLeafDrop());
            }
        }
    }

    private static void register(ColorHandlerEvent.Item event, IItemColor color, net.minecraft.block.Block... blocks) {
        Item[] items = new Item[blocks.length];
        int n = 0;
        for (net.minecraft.block.Block block : blocks) {
            if (block != null) {
                Item item = Item.getItemFromBlock(block);
                if (item != null) {
                    items[n++] = item;
                }
            }
        }
        if (n == 0) {
            return;
        }
        Item[] trimmed = new Item[n];
        System.arraycopy(items, 0, trimmed, 0, n);
        event.getItemColors().registerItemColorHandler(color, trimmed);
    }

    /** 供别处判断某物品是否是本 mod 的（留个口子，避免 ItemStack 空判重复写） */
    public static boolean isOurItem(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem().getRegistryName() == null) {
            return false;
        }
        return NeoTreeOres.MODID.equals(stack.getItem().getRegistryName().getResourceDomain());
    }
}
