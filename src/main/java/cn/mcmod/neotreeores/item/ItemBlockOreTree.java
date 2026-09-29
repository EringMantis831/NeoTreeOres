package cn.mcmod.neotreeores.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * 矿石树方块的物品形式。
 *
 * <p>{@link ItemBlock} 默认显示名走"未本地化名 + {@code .name}"这个 lang key，
 * 而脚本新增树没有 lang 文件，于是会直接暴露 {@code tile.neotreeores.xxx.name}。
 * 这里改走方块自己的 {@code getLocalizedName()}（我们的方块已覆写成
 * "内置树查 lang / 脚本树用 OD 名拼"）。</p>
 */
public class ItemBlockOreTree extends ItemBlock {

    public ItemBlockOreTree(Block block) {
        super(block);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return this.block.getLocalizedName();
    }
}
