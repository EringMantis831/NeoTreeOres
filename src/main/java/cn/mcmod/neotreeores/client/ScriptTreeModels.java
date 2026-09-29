package cn.mcmod.neotreeores.client;

import cn.mcmod.neotreeores.NeoTreeOres;
import cn.mcmod.neotreeores.tree.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import java.util.HashSet;
import java.util.Set;

/** Script trees use bundled shared JSON; removing any built-in tree is safe. */
public final class ScriptTreeModels {
    private ScriptTreeModels() {}

    public static Set<Item> bind() {
        Set<Item> handled = new HashSet<>();
        int count = 0;
        for (IOreTree tree : OreTreeRegistry.allTrees()) {
            if (tree instanceof OreTreeType) continue;
            OreTreeContent c = OreTreeRegistry.get(tree);
            if (c == null) continue;
            String shape = tree.getShape().getTextureBase();
            bindPart(c.getLog(), "log", shape, handled);
            bindPart(c.getWood(), "wood", shape, handled);
            bindPart(c.getLeaves(), "leaves", shape, handled);
            bindPart(c.getSapling(), "sapling", shape, handled);
            bindPart(c.getFallenLeaves(), "fallen_leaves", shape, handled);
            if (c.getLeafDrop() != null) bindItem(c.getLeafDrop(), "dyn_drop_" + shape, handled);
            count++;
        }
        NeoTreeOres.LOGGER.info("[NeoTreeOres] script trees: {} use independent shared JSON models", count);
        return handled;
    }

    private static void bindPart(Block block, String part, String shape, Set<Item> handled) {
        if (block == null) return;
        final String model = "dyn_" + part + "_" + shape;
        ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                return new ModelResourceLocation(NeoTreeOres.MODID + ":" + model,
                        getPropertyString(state.getProperties()));
            }
        });
        bindItem(Item.getItemFromBlock(block), model, handled);
    }

    private static void bindItem(Item item, String model, Set<Item> handled) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(NeoTreeOres.MODID + ":" + model, "inventory"));
        handled.add(item);
    }
}
