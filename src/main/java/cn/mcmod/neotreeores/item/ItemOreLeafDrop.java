package cn.mcmod.neotreeores.item;

import cn.mcmod.neotreeores.tree.IOreTree;
import cn.mcmod.neotreeores.NeoTreeOres;
import cn.mcmod.neotreeores.tree.OreTreeType;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

/**
 * 树叶掉落物：矿物针叶 / 阔叶。
 *
 * <p>云杉形树 → {@code <ore>_needleleaf}（针叶）；橡树形树 → {@code <ore>_broadleaf}（阔叶）。
 * 每种树只会注册与自身树形匹配的那一个。</p>
 */
public class ItemOreLeafDrop extends Item {

    private final IOreTree type;

    public ItemOreLeafDrop(IOreTree type) {
        this.type = type;
        this.setCreativeTab(NeoTreeOres.TAB);
        this.setMaxStackSize(64);
    }

    public IOreTree getTreeType() {
        return type;
    }

    /** 显示名：内置树走 lang 文件（中/英），脚本新增树用 OD 名现场拼（资源包给了 key 就优先用） */
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return cn.mcmod.neotreeores.tree.TreeNames.leafDrop(type);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            items.add(new ItemStack(this));
        }
    }
}
