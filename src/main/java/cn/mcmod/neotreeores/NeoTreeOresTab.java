package cn.mcmod.neotreeores;

import cn.mcmod.neotreeores.tree.OreGate;
import cn.mcmod.neotreeores.tree.OreTreeContent;
import cn.mcmod.neotreeores.tree.OreTreeRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/**
 * NeoTreeOres 创造模式标签页。
 *
 * <p>内容<b>动态过滤</b>：只有"已启用"的树才会出现在这里
 * （{@code N} 的树永远启用；{@code Y} 的树取决于是否检测到对应矿物 OD）。</p>
 */
public class NeoTreeOresTab extends CreativeTabs {

    public NeoTreeOresTab() {
        super(NeoTreeOres.MODID);
    }

    @Override
    public ItemStack getTabIconItem() {
        for (OreTreeContent content : OreTreeRegistry.all()) {
            if (OreGate.isEnabled(content.getType()) && content.getSapling() != null) {
                return new ItemStack(content.getSapling());
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void displayAllRelevantItems(NonNullList<ItemStack> list) {
        for (OreTreeContent content : OreTreeRegistry.all()) {
            if (!OreGate.isEnabled(content.getType())) {
                continue;
            }
            add(list, content.getLog());
            add(list, content.getWood());
            add(list, content.getLeaves());
            add(list, content.getSapling());
            add(list, content.getFallenLeaves());
            add(list, content.getLeafDrop());
        }
    }

    private static void add(NonNullList<ItemStack> list, net.minecraft.item.Item item) {
        if (item != null) {
            list.add(new ItemStack(item));
        }
    }

    private static void add(NonNullList<ItemStack> list, net.minecraft.block.Block block) {
        if (block != null) {
            net.minecraft.item.Item item = net.minecraft.item.Item.getItemFromBlock(block);
            if (item != null) {
                list.add(new ItemStack(item));
            }
        }
    }
}
