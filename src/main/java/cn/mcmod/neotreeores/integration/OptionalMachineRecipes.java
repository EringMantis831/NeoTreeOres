package cn.mcmod.neotreeores.integration;

import cn.mcmod.neotreeores.NeoTreeOres;
import cn.mcmod.neotreeores.tree.IOreTree;
import cn.mcmod.neotreeores.tree.OreTreeContent;
import cn.mcmod.neotreeores.tree.OreTreeRegistry;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Optional Mekanism CEU / Thermal Expansion recipe bridges. */
public final class OptionalMachineRecipes {
    private OptionalMachineRecipes() {}

    private static boolean mekRegistered;
    private static boolean thermalRegistered;

    public static void registerAll() {
        registerMekCEU();
        registerMekEnergy();
        registerThermalExpansion();
    }

    private static void registerMekCEU() {
        if (mekRegistered) return;
        try {
            Class<?> handler = Class.forName("mekanism.common.recipe.RecipeHandler");
            Class<?> fluids = Class.forName("mekanism.common.MekanismFluids");
            Class<?> chance = Class.forName("mekanism.api.recipes.FarmChanceOutput");
            Class<?> fluidStack = Class.forName("net.minecraftforge.fluids.FluidStack");
            Object nutrient = fluids.getField("NutrientSolution").get(null);
            Method gasRecipe = find(handler, "addOrganicFarmRecipe", ItemStack.class, Class.forName("mekanism.api.gas.Gas"), ItemStack.class, List.class);
            Method waterRecipe = find(handler, "addOrganicFarmRecipe", ItemStack.class, fluidStack, ItemStack.class, List.class);
            Constructor<?> chanceCtor = chance.getConstructor(ItemStack.class, double.class);
            Class<?> forgeFluid = Class.forName("net.minecraftforge.fluids.FluidRegistry");
            Object water = forgeFluid.getField("WATER").get(null);
            Constructor<?> fluidCtor = fluidStack.getConstructor(Class.forName("net.minecraftforge.fluids.Fluid"), int.class);
            int n = 0;
            for (IOreTree tree : OreTreeRegistry.allTrees()) {
                OreTreeContent c = OreTreeRegistry.get(tree);
                if (c == null || c.getSapling() == null || c.getLog() == null || c.getLeafDrop() == null) continue;
                ItemStack sapling = new ItemStack(c.getSapling());
                List<Object> nutrientDrops = new ArrayList<Object>();
                nutrientDrops.add(chanceCtor.newInstance(new ItemStack(c.getSapling(), 4), 1.0D));
                nutrientDrops.add(chanceCtor.newInstance(new ItemStack(c.getLeafDrop()), 0.60D));
                gasRecipe.invoke(null, sapling, nutrient, new ItemStack(c.getLog(), 24), nutrientDrops);
                List<Object> waterDrops = new ArrayList<Object>();
                waterDrops.add(chanceCtor.newInstance(new ItemStack(c.getSapling()), 1.0D));
                waterDrops.add(chanceCtor.newInstance(new ItemStack(c.getLeafDrop()), 0.15D));
                waterRecipe.invoke(null, sapling, fluidCtor.newInstance(water, 1), new ItemStack(c.getLog(), 6), waterDrops);
                n++;
            }
            mekRegistered = true;
            NeoTreeOres.LOGGER.info("[NeoTreeOres] Mekanism CEU organic farm: {} trees (saplings + leaves)", Integer.valueOf(n));
        } catch (ClassNotFoundException e) {
            NeoTreeOres.LOGGER.debug("[NeoTreeOres] Mekanism CEU not installed; skipping organic farm recipes");
        } catch (Throwable e) {
            NeoTreeOres.LOGGER.warn("[NeoTreeOres] Mekanism CEU integration failed", e);
        }
    }

    private static void registerMekEnergy() {
        try {
            Class<?> handler = Class.forName("mekanism.common.recipe.RecipeHandler");
            Class<?> types = Class.forName("mekanism.common.recipe.RecipeHandler$Recipe");
            Object redstoneTree = cn.mcmod.neotreeores.tree.OreTreeType.REDSTONE;
            OreTreeContent c = OreTreeRegistry.get((IOreTree) redstoneTree);
            if (c == null || c.getLeafDrop() == null) return;
            Class<?> config = Class.forName("mekanism.common.config.MekanismConfig");
            Object current = config.getMethod("current").invoke(null);
            Object general = current.getClass().getField("general").get(current);
            Object option = general.getClass().getField("ENERGY_PER_REDSTONE").get(general);
            double redstone = ((Number) option.getClass().getMethod("val").invoke(option)).doubleValue();
            handler.getMethod("addItemStackToEnergyRecipe", ItemStack.class, double.class)
                    .invoke(null, new ItemStack(c.getLeafDrop()), redstone * 5.0D);
            NeoTreeOres.LOGGER.info("[NeoTreeOres] Mekanism CEU redstone needleleaf energy recipe: {} RF",
                    Double.valueOf(redstone * 5.0D));
        } catch (ClassNotFoundException e) {
            NeoTreeOres.LOGGER.debug("[NeoTreeOres] Mekanism CEU not installed; skipping energy recipe");
        } catch (Throwable e) {
            NeoTreeOres.LOGGER.warn("[NeoTreeOres] Mekanism CEU energy integration failed", e);
        }
    }

    private static void registerThermalExpansion() {
        if (thermalRegistered) return;
        try {
            Class<?> manager = Class.forName("cofh.thermalexpansion.util.managers.machine.InsolatorManager");
            Method add = manager.getMethod("addDefaultTreeRecipe", ItemStack.class, ItemStack.class, ItemStack.class);
            int n = 0;
            for (IOreTree tree : OreTreeRegistry.allTrees()) {
                OreTreeContent c = OreTreeRegistry.get(tree);
                if (c == null || c.getSapling() == null || c.getLog() == null) continue;
                add.invoke(null, new ItemStack(c.getSapling()), new ItemStack(c.getLog(), 6), new ItemStack(c.getSapling()));
                n++;
            }
            thermalRegistered = true;
            NeoTreeOres.LOGGER.info("[NeoTreeOres] Thermal Expansion insolator: {} trees", Integer.valueOf(n));
        } catch (ClassNotFoundException e) {
            NeoTreeOres.LOGGER.debug("[NeoTreeOres] Thermal Expansion not installed; skipping insolator recipes");
        } catch (Throwable e) {
            NeoTreeOres.LOGGER.warn("[NeoTreeOres] Thermal Expansion integration failed", e);
        }
    }

    private static Method find(Class<?> type, String name, Class<?>... params) throws NoSuchMethodException {
        return type.getMethod(name, params);
    }
}
