package cn.mcmod.neotreeores.integration.jei;

import cn.mcmod.neotreeores.recipe.OreTreeRecipes;
import cn.mcmod.neotreeores.tree.IOreTree;
import cn.mcmod.neotreeores.tree.OreTreeContent;
import cn.mcmod.neotreeores.tree.OreTreeRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Optional JEI bridge for NeoTreeOres, Mekanism CEU and Thermal Expansion. */
@JEIPlugin
public final class NeoTreeOresJeiPlugin implements IModPlugin {
    @Override
    public void register(IModRegistry registry) {
        registerNeoSmelting(registry);
        // Mekanism, Thermal Expansion and BonsaiTrees already expose their own
        // manager recipes through their own JEI plugins. Do not register them
        // here again: duplicate registration corrupts the shared JEI category.
    }

    private static void registerNeoSmelting(IModRegistry registry) {
        Collection<IRecipeWrapper> recipes = new ArrayList<IRecipeWrapper>();
        for (IOreTree tree : OreTreeRegistry.allTrees()) {
            OreTreeContent content = OreTreeRegistry.get(tree);
            ItemStack output = OreTreeRecipes.firstLogNugget(tree, content);
            if (output.isEmpty()) continue;
            List<ItemStack> inputs = new ArrayList<ItemStack>();
            for (int metadata : new int[] {0, 4, 8, 12}) {
                inputs.add(new ItemStack(content.getLog(), 1, metadata));
            }
            recipes.add(registry.getJeiHelpers().getVanillaRecipeFactory()
                    .createSmeltingRecipe(inputs, output));
        }
        if (!recipes.isEmpty()) {
            registry.addRecipes(recipes, VanillaRecipeCategoryUid.SMELTING);
        }
    }

    /* External machine JEI registration intentionally removed. */
    private static void registerMekanismFarm(IModRegistry registry) {
        try {
            Class<?> recipeEnum = Class.forName("mekanism.common.recipe.RecipeHandler$Recipe");
            Object organic = recipeEnum.getField("ORGANIC_FARM").get(null);
            Method get = organic.getClass().getMethod("get");
            Object map = get.invoke(organic);
            if (!(map instanceof java.util.Map)) return;
            Class<?> wrapper = Class.forName("mekanism.client.jei.machine.FarmMachineRecipeWrapper");
            Collection<IRecipeWrapper> wrappers = new ArrayList<IRecipeWrapper>();
            java.lang.reflect.Constructor<?> ctor = wrapper.getConstructors()[0];
            for (Object recipe : ((java.util.Map<?, ?>) map).values()) {
                wrappers.add((IRecipeWrapper) ctor.newInstance(recipe));
            }
            Method category = recipeEnum.getMethod("getJEICategory");
            String uid = (String) category.invoke(organic);
            if (!wrappers.isEmpty()) registry.addRecipes(wrappers, uid);
        } catch (Throwable ignored) {
            // Mekanism CEU is optional; absent or incompatible versions are ignored.
        }
    }

    private static void registerMekanismEnergy(IModRegistry registry) {
        try {
            Class<?> recipeEnum = Class.forName("mekanism.common.recipe.RecipeHandler$Recipe");
            Object energy = recipeEnum.getField("ENERGY_RECIPE").get(null);
            Object map = energy.getClass().getMethod("get").invoke(energy);
            if (!(map instanceof java.util.Map)) return;
            Class<?> wrapper = Class.forName("mekanism.client.jei.machine.other.ItemStackToEnergyRecipeWrapper");
            java.lang.reflect.Constructor<?> ctor = wrapper.getConstructors()[0];
            Collection<IRecipeWrapper> wrappers = new ArrayList<IRecipeWrapper>();
            for (Object recipe : ((java.util.Map<?, ?>) map).values()) {
                wrappers.add((IRecipeWrapper) ctor.newInstance(recipe));
            }
            String uid = (String) recipeEnum.getMethod("getJEICategory").invoke(energy);
            if (!wrappers.isEmpty()) registry.addRecipes(wrappers, uid);
        } catch (Throwable ignored) {
            // Mekanism CEU is optional.
        }
    }

    private static void registerThermalInsolator(IModRegistry registry) {
        try {
            Class<?> manager = Class.forName("cofh.thermalexpansion.util.managers.machine.InsolatorManager");
            Method listMethod = manager.getMethod("getRecipeList");
            Object recipes = listMethod.invoke(null);
            Class<?> recipeClass = Class.forName("cofh.thermalexpansion.util.managers.machine.InsolatorManager$InsolatorRecipe");
            Class<?> typeClass = Class.forName("cofh.thermalexpansion.util.managers.machine.InsolatorManager$Type");
            Object treeType = Enum.valueOf((Class) typeClass, "TREE");
            Method getType = recipeClass.getMethod("getType");
            Object guiHelper = registry.getJeiHelpers().getGuiHelper();
            Class<?> wrapper = Class.forName("cofh.thermalexpansion.plugins.jei.machine.insolator.InsolatorRecipeWrapper");
            Collection<IRecipeWrapper> wrappers = new ArrayList<IRecipeWrapper>();
            for (Object recipe : (Collection<?>) recipes) {
                if (getType.invoke(recipe) != treeType) continue;
                java.lang.reflect.Constructor<?> chosen = null;
                for (java.lang.reflect.Constructor<?> candidate : wrapper.getConstructors()) {
                    if (candidate.getParameterTypes().length == 3) { chosen = candidate; break; }
                }
                if (chosen == null) continue;
                wrappers.add((IRecipeWrapper) chosen.newInstance(guiHelper, recipe, "thermalexpansion.insolator_tree"));
            }
            if (!wrappers.isEmpty()) registry.addRecipes(wrappers, "thermalexpansion.insolator_tree");
        } catch (Throwable ignored) {
            // Thermal Expansion is optional; absent or incompatible versions are ignored.
        }
    }

    private static void registerBonsaiTrees(IModRegistry registry) {
        try {
            Class<?> bonsai = Class.forName("org.dave.bonsaitrees.BonsaiTrees");
            Object instance = bonsai.getField("instance").get(null);
            Object typeRegistry = bonsai.getField("typeRegistry").get(instance);
            Collection<?> types = (Collection<?>) typeRegistry.getClass().getMethod("getAllTypes").invoke(typeRegistry);
            Class<?> wrapper = Class.forName("org.dave.bonsaitrees.jei.BonsaiTreeRecipeWrapper");
            java.lang.reflect.Constructor<?> ctor = wrapper.getConstructors()[0];
            Collection<IRecipeWrapper> recipes = new ArrayList<IRecipeWrapper>();
            for (Object type : types) {
                recipes.add((IRecipeWrapper) ctor.newInstance(type));
            }
            if (!recipes.isEmpty()) registry.addRecipes(recipes, "bonsaitrees.Growing");
        } catch (Throwable ignored) {
            // BonsaiTrees is optional; its own JEI plugin may be absent or incompatible.
        }
    }
}
