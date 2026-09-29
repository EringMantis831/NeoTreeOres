package cn.mcmod.neotreeores.recipe;

import cn.mcmod.neotreeores.tree.CtTrees;
import cn.mcmod.neotreeores.tree.IOreTree;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

/** Normal shapeless recipe, with the same dimension policy used by its sapling. */
public final class DimensionLeavesRecipe extends ShapelessRecipes {
    private final IOreTree tree;

    public DimensionLeavesRecipe(IOreTree tree, ItemStack result, NonNullList<Ingredient> ingredients) {
        super("", result, ingredients);
        this.tree = tree;
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return world != null && CtTrees.allowsDimension(tree, world.provider.getDimension())
                && super.matches(inventory, world);
    }
}
