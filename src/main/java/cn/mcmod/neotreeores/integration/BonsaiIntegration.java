package cn.mcmod.neotreeores.integration;

import cn.mcmod.neotreeores.NeoTreeOres;
import cn.mcmod.neotreeores.tree.IOreTree;
import cn.mcmod.neotreeores.tree.OreTreeContent;
import cn.mcmod.neotreeores.tree.OreTreeRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/** Optional BonsaiTrees 1.12 integration: types, drops and a JEI-visible shape. */
public final class BonsaiIntegration {
    private static boolean done;
    private BonsaiIntegration() {}

    public static void registerAll() {
        if (done) return;
        try {
            Class<?> bonsai = Class.forName("org.dave.bonsaitrees.BonsaiTrees");
            Object instance = bonsai.getField("instance").get(null);
            Object typeRegistry = bonsai.getField("typeRegistry").get(instance);
            Class<?> treeType = Class.forName("org.dave.bonsaitrees.api.TreeTypeSimple");
            Class<?> integration = Class.forName("org.dave.bonsaitrees.api.IBonsaiIntegration");
            Method register = typeRegistry.getClass().getMethod("registerTreeType", integration, Class.forName("org.dave.bonsaitrees.api.IBonsaiTreeType"));
            Class<?> shape = Class.forName("org.dave.bonsaitrees.trees.TreeShape");
            Class<?> shapeRegistry = Class.forName("org.dave.bonsaitrees.trees.TreeShapeRegistry");
            Field byFile = shapeRegistry.getDeclaredField("treeShapesByFilename");
            Field byType = shapeRegistry.getDeclaredField("treeShapesByType");
            byFile.setAccessible(true); byType.setAccessible(true);
            Map<String, Object> shapesByFile = (Map<String, Object>) byFile.get(null);
            Map<Object, List<Object>> shapesByType = (Map<Object, List<Object>>) byType.get(null);
            Object integrationProxy = Proxy.newProxyInstance(integration.getClassLoader(), new Class<?>[]{integration}, (p,m,a) -> null);
            // BonsaiTrees 靠“标签”把树和土壤配起来：树声明自己能长在哪些标签的土上，
            // 原版 config/soils.d/dirt.json 与 grass.json 提供的标签正是 dirt 与 grass。
            Method addSoilTag = treeType.getMethod("addCompatibleSoilTag", String.class);
            for (IOreTree tree : OreTreeRegistry.allTrees()) {
                OreTreeContent c = OreTreeRegistry.get(tree);
                if (c == null) continue;
                String name = "neotreeores:" + tree.getId();
                Object t = treeType.getConstructor(String.class, ItemStack.class).newInstance(name, new ItemStack(c.getSapling()));
                addSoilTag.invoke(t, "dirt");
                addSoilTag.invoke(t, "grass");
                Method addDrop = treeType.getMethod("addDrop", ItemStack.class, float.class);
                addDrop.invoke(t, new ItemStack(c.getLog()), 0.75F);
                addDrop.invoke(t, new ItemStack(c.getLeaves()), 0.30F);
                addDrop.invoke(t, new ItemStack(c.getLeafDrop()), 0.20F);
                addDrop.invoke(t, new ItemStack(c.getFallenLeaves()), 0.05F);
                addDrop.invoke(t, new ItemStack(c.getSapling()), 0.01F);
                register.invoke(typeRegistry, integrationProxy, t);
                String vanillaName = tree.getShape() == cn.mcmod.neotreeores.tree.OreTreeType.Shape.SPRUCE
                        ? "minecraft:spruce" : "minecraft:oak";
                Method getTypeByName = typeRegistry.getClass().getMethod("getTypeByName", String.class);
                Object vanillaType = getTypeByName.invoke(typeRegistry, vanillaName);
                List<Object> vanillaShapes = vanillaType == null ? null : shapesByType.get(vanillaType);
                if (vanillaShapes == null || vanillaShapes.isEmpty()) {
                    NeoTreeOres.LOGGER.warn("[NeoTreeOres] Bonsai: no vanilla {} shape for {}",
                            vanillaName, tree.getId());
                    continue;
                }
                Object sourceShape = vanillaShapes.get(0);
                Map<BlockPos, IBlockState> sourceBlocks = (Map<BlockPos, IBlockState>) shape
                        .getMethod("getBlocks").invoke(sourceShape);
                Map<BlockPos, IBlockState> blocks = new HashMap<BlockPos, IBlockState>();
                for (Map.Entry<BlockPos, IBlockState> entry : sourceBlocks.entrySet()) {
                    IBlockState state = entry.getValue();
                    net.minecraft.block.Block block = state.getBlock();
                    if (block == net.minecraft.init.Blocks.LOG || block == net.minecraft.init.Blocks.LOG2) {
                        state = c.getLog().getDefaultState();
                    } else if (block == net.minecraft.init.Blocks.LEAVES || block == net.minecraft.init.Blocks.LEAVES2) {
                        state = c.getLeaves().getDefaultState();
                    }
                    blocks.put(entry.getKey(), state);
                }
                Object sh = shape.getConstructor(String.class).newInstance(name);
                shape.getMethod("setBlocks", Map.class).invoke(sh, blocks);
                String file = "neotreeores_" + tree.getId() + "001";
                shape.getMethod("setFileName", String.class).invoke(sh, file);
                shapesByFile.put(file, sh);
                shapesByType.computeIfAbsent(t, k -> new ArrayList<Object>()).add(sh);
            }
            rebuildSoilCompatibility(bonsai, instance, typeRegistry);
            done = true;
            NeoTreeOres.LOGGER.info("[NeoTreeOres] BonsaiTrees integration registered");
        } catch (ClassNotFoundException e) {
            NeoTreeOres.LOGGER.debug("[NeoTreeOres] BonsaiTrees not installed; skipping bonsai recipes");
        } catch (Throwable e) {
            NeoTreeOres.LOGGER.warn("[NeoTreeOres] BonsaiTrees integration failed", e);
        }
    }

    /**
     * 重建 BonsaiTrees 的「树 ↔ 土壤」兼容表。
     *
     * <p>BonsaiTrees 在它自己的 postInit 里（{@code SoilCompatibility.updateCompatibility}）
     * 就把这张表算完了，那时本模组还没注册；而我们用 {@code after:bonsaitrees} 保证自己排在它之后，
     * 所以矿树不会出现在表里。后果是 JEI 的盆栽配方看不到土壤槽、盆栽也判定为不能种。
     * 注册完所有树后重算一次即可（{@code updateCompatibility} 本身是幂等的）。</p>
     */
    private static void rebuildSoilCompatibility(Class<?> bonsai, Object instance, Object typeRegistry) throws Exception {
        Object soilRegistry = bonsai.getField("soilRegistry").get(instance);
        Object soilCompatibility = bonsai.getField("soilCompatibility").get(instance);
        if (soilRegistry == null || soilCompatibility == null) {
            NeoTreeOres.LOGGER.warn("[NeoTreeOres] Bonsai: soil registry not ready yet; "
                    + "mineral trees have no compatible soil until the game is restarted");
            return;
        }
        Class<?> soilRegistryClass = Class.forName("org.dave.bonsaitrees.soils.BonsaiSoilRegistry");
        Class<?> typeRegistryClass = Class.forName("org.dave.bonsaitrees.trees.TreeTypeRegistry");
        Method update = soilCompatibility.getClass().getMethod("updateCompatibility", soilRegistryClass, typeRegistryClass);
        update.invoke(soilCompatibility, soilRegistry, typeRegistry);
        NeoTreeOres.LOGGER.info("[NeoTreeOres] Bonsai: soil compatibility rebuilt (dirt/grass)");
    }
}
