package cn.mcmod.neotreeores.tree;

import cn.mcmod.neotreeores.recipe.OreTreeRecipes;
import net.minecraft.init.Bootstrap;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

/** Exercises the real block/item/recipe registration without launching a client. */
public final class CtRegistryRegression {
    public static void main(String[] args) throws Exception {
        CtControlsRegression.main(args);
        Bootstrap.register();
        verifyNamesAndOrePresence();
        OreTreeRegistry.registerBlocks(new RegistryEvent.Register<Block>(new ResourceLocation("minecraft:blocks"), ForgeRegistries.BLOCKS));
        OreTreeRegistry.registerItems(new RegistryEvent.Register<Item>(new ResourceLocation("minecraft:items"), ForgeRegistries.ITEMS));
        if (ForgeRegistries.BLOCKS.containsKey(new ResourceLocation("neotreeores:diamond_sapling")))
            throw new AssertionError("Diamond block still registered");
        if (ForgeRegistries.ITEMS.containsKey(new ResourceLocation("neotreeores:diamond_needleleaf")))
            throw new AssertionError("Diamond item still registered");
        // The harness has no FML startup ore-init event; provide vanilla output entries.
        if (OreDictionary.getOres("gemEmerald", false).isEmpty()) OreDictionary.registerOre("gemEmerald", net.minecraft.init.Items.EMERALD);
        if (OreDictionary.getOres("gemQuartz", false).isEmpty()) OreDictionary.registerOre("gemQuartz", net.minecraft.init.Items.QUARTZ);
        OreTreeRecipes.registerRecipes(new RegistryEvent.Register<IRecipe>(new ResourceLocation("minecraft:recipes"), ForgeRegistries.RECIPES));
        IRecipe emerald = ForgeRegistries.RECIPES.getValue(new ResourceLocation("neotreeores:emerald_leaves_to_ore"));
        if (emerald == null || emerald.getIngredients().size() != 1 || emerald.getRecipeOutput().getCount() != 64)
            throw new AssertionError("Actual Emerald recipe is not 1:64");
        if (emerald.getRecipeOutput().getItem() != net.minecraft.init.Items.EMERALD)
            throw new AssertionError("Actual Emerald output not emerald");
        if (ForgeRegistries.RECIPES.containsKey(new ResourceLocation("neotreeores:diamond_leaves_to_ore")))
            throw new AssertionError("Diamond recipe still registered");
        ItemStack quartz = new ItemStack(OreTreeRegistry.get(CtTrees.findByOre("Quartz")).getSapling());
        if (quartz.isEmpty()) throw new AssertionError("Quartz addition regressed");
        verifyDimensions(emerald);
        verifyCarpetDrops();
        verifyTreeObstructions();
        verifyTreeEnvelope();
        verifyBlockedSaplings();
        System.out.println("PASS CtRegistryRegression: Diamond absent; Emerald 1:64; Quartz present; carpet drops, dimensions and oak/mega-spruce obstructions passed");
    }

    private static void verifyNamesAndOrePresence() throws Exception {
        java.lang.reflect.Method append = cn.mcmod.neotreeores.client.CtTreeNames.class
                .getDeclaredMethod("append", StringBuilder.class, String.class, String.class);
        append.setAccessible(true);
        String key = "tile.neotreeores.regression_localized.name";
        net.minecraft.util.text.translation.LanguageMap.inject(new java.io.ByteArrayInputStream(
                (key + "=Custom translated name\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        StringBuilder names = new StringBuilder();
        append.invoke(null, names, key, "Generated English");
        if (names.length() != 0) throw new AssertionError("Existing translation would be overwritten");
        append.invoke(null, names, key + "_missing", "Generated English");
        if (!names.toString().contains("=Generated English")) throw new AssertionError("Missing fallback");
        java.lang.reflect.Method detect = OreGate.class.getDeclaredMethod("detect", IOreTree.class);
        detect.setAccessible(true);
        CtOreTree tree = new CtOreTree("RegressionAbsentMineral", 0, 0, OreTreeType.Shape.OAK, 1, 1, 0, 2F, null, false);
        String ore = "ingotRegressionAbsentMineral";
        if ((Boolean) detect.invoke(null, tree) || OreDictionary.doesOreNameExist(ore))
            throw new AssertionError("Absent ore accepted or created by lookup");
        OreDictionary.getOreID(ore);
        if (!OreDictionary.doesOreNameExist(ore) || (Boolean) detect.invoke(null, tree))
            throw new AssertionError("Empty historical name accepted");
        OreDictionary.registerOre(ore, net.minecraft.init.Items.IRON_INGOT);
        if (!(Boolean) detect.invoke(null, tree)) throw new AssertionError("Live ore rejected");
        System.out.println("PASS localization fallback preserves translations; absent/empty/live ore detection");
    }

    private static void verifyTreeObstructions() {
        TestWorld world = new TestWorld(0);
        net.minecraft.util.math.BlockPos origin = new net.minecraft.util.math.BlockPos(0, 64, 0);
        net.minecraft.util.math.BlockPos[] single = {origin};
        net.minecraft.util.math.BlockPos[] four = {origin, origin.east(), origin.south(), origin.east().south()};
        net.minecraft.world.gen.feature.WorldGenerator oak = OreTreeGenerators.createFor(OreTreeType.IRON, new java.util.Random(1));
        net.minecraft.world.gen.feature.WorldGenerator fancy = new cn.mcmod.neotreeores.world.gen.WorldGenOreFancyOak(true,
                OreTreeRegistry.get(OreTreeType.IRON).getLog().getDefaultState(),
                OreTreeRegistry.get(OreTreeType.IRON).getLeaves().getDefaultState());
        net.minecraft.world.gen.feature.WorldGenerator mega = OreTreeGenerators.createFor(OreTreeType.EMERALD,
                new java.util.Random(2), true);
        if (!(oak instanceof cn.mcmod.neotreeores.world.gen.WorldGenOreOak)
                || !(mega instanceof cn.mcmod.neotreeores.world.gen.WorldGenOreMegaPine))
            throw new AssertionError("The intended oak/mega generators are not in use");
        for (net.minecraft.util.math.BlockPos p : four) world.blocks.put(p, OreTreeRegistry.get(OreTreeType.EMERALD).getSapling().getDefaultState());
        if (!OreTreeGenerators.canGrowInto(world, origin, mega, four))
            throw new AssertionError("The four saplings must not obstruct their own mega tree");
        world.blocks.clear();
        if (!OreTreeGenerators.canGrowInto(world, origin, oak, single)
                || !OreTreeGenerators.canGrowInto(world, origin, fancy, single)
                || !OreTreeGenerators.canGrowInto(world, origin, mega, four))
            throw new AssertionError("Clear space rejected");
        // Old vanilla checks miss existing wood, and BigTree only checks the trunk line.
        for (net.minecraft.world.gen.feature.WorldGenerator tree :
                new net.minecraft.world.gen.feature.WorldGenerator[] {oak, fancy, mega}) {
            net.minecraft.util.math.BlockPos[] saplings = tree == mega ? four : single;
            for (net.minecraft.util.math.BlockPos blocked :
                    new net.minecraft.util.math.BlockPos[] {origin.up(2), origin.add(2, 10, 0), origin.add(5, 16, 0)}) {
                if (tree == oak && blocked.getX() > 2 || tree == fancy && blocked.getX() > 5
                        || tree == oak && blocked.getY() - origin.getY() > 8) continue;
                world.blocks.put(blocked, net.minecraft.init.Blocks.STONE.getDefaultState());
                if (OreTreeGenerators.canGrowInto(world, origin, tree, saplings))
                    throw new AssertionError("Obstruction missed: " + tree.getClass().getSimpleName() + " " + blocked);
                world.blocks.put(blocked, net.minecraft.init.Blocks.LOG.getDefaultState());
                if (OreTreeGenerators.canGrowInto(world, origin, tree, saplings))
                    throw new AssertionError("Player log accepted: " + tree.getClass().getSimpleName());
                world.blocks.remove(blocked);
            }
        }
        // Trunk and canopy may replace leaves, preserving the existing vanilla behavior.
        world.blocks.put(origin.up(2), net.minecraft.init.Blocks.LEAVES.getDefaultState());
        if (!OreTreeGenerators.canGrowInto(world, origin, oak, single))
            throw new AssertionError("Existing leaves should remain replaceable");
    }

    private static void verifyTreeEnvelope() {
        net.minecraft.util.math.BlockPos origin = new net.minecraft.util.math.BlockPos(0, 64, 0);
        for (int i = 0; i < 100; i++) {
            TestWorld world = new TestWorld(0);
            for (int x = -2; x <= 3; x++) for (int z = -2; z <= 3; z++)
                world.blocks.put(origin.add(x, -1, z), net.minecraft.init.Blocks.DIRT.getDefaultState());
            net.minecraft.world.gen.feature.WorldGenerator[] trees = {
                    new cn.mcmod.neotreeores.world.gen.WorldGenOreOak(
                            OreTreeRegistry.get(OreTreeType.IRON).getLog().getDefaultState(),
                            OreTreeRegistry.get(OreTreeType.IRON).getLeaves().getDefaultState()),
                    new cn.mcmod.neotreeores.world.gen.WorldGenOreFancyOak(true,
                            OreTreeRegistry.get(OreTreeType.IRON).getLog().getDefaultState(),
                            OreTreeRegistry.get(OreTreeType.IRON).getLeaves().getDefaultState()),
                    new cn.mcmod.neotreeores.world.gen.WorldGenOreMegaPine(false,
                            OreTreeRegistry.get(OreTreeType.EMERALD).getLog().getDefaultState(),
                            OreTreeRegistry.get(OreTreeType.EMERALD).getLeaves().getDefaultState()),
                    new cn.mcmod.neotreeores.world.gen.WorldGenOreMegaPine(true,
                            OreTreeRegistry.get(OreTreeType.EMERALD).getLog().getDefaultState(),
                            OreTreeRegistry.get(OreTreeType.EMERALD).getLeaves().getDefaultState())
            };
            for (net.minecraft.world.gen.feature.WorldGenerator tree : trees) {
                world.blocks.entrySet().removeIf(e -> e.getKey().getY() >= origin.getY());
                net.minecraft.util.math.BlockPos[] saplings = tree instanceof cn.mcmod.neotreeores.world.gen.WorldGenOreMegaPine
                        ? new net.minecraft.util.math.BlockPos[]{origin, origin.east(), origin.south(), origin.east().south()}
                        : new net.minecraft.util.math.BlockPos[]{origin};
                if (!OreTreeGenerators.canGrowInto(world, origin, tree, saplings)) throw new AssertionError("Clear space rejected");
                if (!tree.generate(world, new java.util.Random(i), origin))
                    throw new AssertionError("Generator failed with clear soil: " + tree.getClass().getSimpleName());
                for (net.minecraft.util.math.BlockPos p : world.blocks.keySet()) {
                    if (p.getY() < origin.getY()) continue;
                    boolean mega = tree instanceof cn.mcmod.neotreeores.world.gen.WorldGenOreMegaPine;
                    boolean fancy = tree instanceof cn.mcmod.neotreeores.world.gen.WorldGenOreFancyOak;
                    int min = mega ? -7 : fancy ? -8 : -2;
                    int max = mega ? 8 : fancy ? 8 : 2;
                    int height = mega ? 31 : fancy ? 20 : 8;
                    if (p.getX() < min || p.getX() > max || p.getZ() < min || p.getZ() > max || p.getY() > origin.getY() + height)
                        throw new AssertionError("Tree writes outside checked canopy: " + tree.getClass().getSimpleName() + " " + p);
                }
            }
        }
    }

    private static void verifyBlockedSaplings() {
        net.minecraft.util.math.BlockPos origin = new net.minecraft.util.math.BlockPos(0, 64, 0);
        for (OreTreeType type : new OreTreeType[]{OreTreeType.IRON, OreTreeType.EMERALD}) {
            boolean mega = type == OreTreeType.EMERALD;
            net.minecraft.util.math.BlockPos[] positions = mega
                    ? new net.minecraft.util.math.BlockPos[]{origin, origin.east(), origin.south(), origin.east().south()}
                    : new net.minecraft.util.math.BlockPos[]{origin};
            cn.mcmod.neotreeores.block.BlockOreSapling sapling = OreTreeRegistry.get(type).getSapling();
            for (int seed = 0; seed < 100; seed++) {
                TestWorld world = new TestWorld(0);
                for (net.minecraft.util.math.BlockPos p : positions) {
                    world.blocks.put(p.down(), net.minecraft.init.Blocks.DIRT.getDefaultState());
                    world.blocks.put(p, sapling.getDefaultState());
                }
                world.blocks.put(origin.up(2), net.minecraft.init.Blocks.LOG.getDefaultState());
                java.util.Map<net.minecraft.util.math.BlockPos, net.minecraft.block.state.IBlockState> before =
                        new java.util.HashMap<>(world.blocks);
                sapling.grow(world, new java.util.Random(seed), origin, sapling.getDefaultState());
                if (!before.equals(world.blocks))
                    throw new AssertionError("Blocked growth changed saplings or surrounding blocks: " + type + " seed=" + seed);
            }
        }
        System.out.println("PASS growth: 400 generated trees (100 seeds per shape); 200 blocked sapling attempts unchanged");
    }

    private static void verifyCarpetDrops() {
        OreTreeContent content = OreTreeRegistry.get(OreTreeType.IRON);
        TestWorld world = new TestWorld(0);
        net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(0, 64, 0);
        net.minecraft.block.state.IBlockState carpet = content.getFallenLeaves().getDefaultState();
        if (carpet.getMaterial() != net.minecraft.block.material.Material.SNOW
                || !content.getFallenLeaves().isReplaceable(world, pos))
            throw new AssertionError("Carpet no longer has snow-like liquid replacement behavior");
        if (!content.getFallenLeaves().canHarvestBlock(world, pos, null))
            throw new AssertionError("Snow material incorrectly requires a tool to harvest carpet");
        for (int fortune : new int[]{0, 1, 3}) {
            java.util.List<ItemStack> drops = content.getFallenLeaves().getDrops(world, pos, carpet, fortune);
            if (drops.size() != 1 || drops.get(0).getItem() != content.getLeafDrop()
                    || drops.get(0).getCount() < 1
                    || drops.get(0).getCount() > 1 + fortune * Math.max(1, cn.mcmod.neotreeores.NeoTreeOresConfig.carpetFortuneBonus))
                throw new AssertionError("Carpet drop or fortune amount failed: " + fortune);
        }
    }

    private static void verifyDimensions(IRecipe emerald) {
        net.minecraft.inventory.Container container = new net.minecraft.inventory.Container() {
            @Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) { return true; }
        };
        net.minecraft.inventory.InventoryCrafting grid = new net.minecraft.inventory.InventoryCrafting(container, 3, 3);
        OreTreeContent content = OreTreeRegistry.get(OreTreeType.EMERALD);
        grid.setInventorySlotContents(0, new ItemStack(content.getLeafDrop()));
        TestWorld overworld = new TestWorld(0);
        TestWorld nether = new TestWorld(-1);
        if (!emerald.matches(grid, overworld) || emerald.matches(grid, nether))
            throw new AssertionError("Emerald recipe dimension gate not enforced");
        if (emerald.getCraftingResult(grid).getCount() != 64) throw new AssertionError("Crafting output amount");
        net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(0, 64, 0);
        overworld.blocks.put(pos.down(), net.minecraft.init.Blocks.DIRT.getDefaultState());
        nether.blocks.put(pos.down(), net.minecraft.init.Blocks.DIRT.getDefaultState());
        if (!content.getSapling().canPlaceBlockAt(overworld, pos) || content.getSapling().canPlaceBlockAt(nether, pos))
            throw new AssertionError("Built-in sapling placement dimension gate not enforced");
        if (content.getSapling().canGrow(nether, pos, content.getSapling().getDefaultState(), false)
                || content.getSapling().canUseBonemeal(nether, new java.util.Random(0), pos, content.getSapling().getDefaultState()))
            throw new AssertionError("Forbidden sapling still accepts growth/bonemeal");
        OreTreeContent quartz = OreTreeRegistry.get(CtTrees.findByOre("Quartz"));
        if (quartz.getSapling().canPlaceBlockAt(overworld, pos) || !quartz.getSapling().canPlaceBlockAt(nether, pos))
            throw new AssertionError("Existing script sapling whitelist regressed");
        grid.setInventorySlotContents(0, new ItemStack(quartz.getLeafDrop()));
        if (emerald.matches(grid, overworld)) throw new AssertionError("Recipe accepts another species' leaf");
    }

    /** In-memory block access only; never creates a world folder or a client. */
    private static final class TestWorld extends net.minecraft.world.World {
        final java.util.Map<net.minecraft.util.math.BlockPos, net.minecraft.block.state.IBlockState> blocks = new java.util.HashMap<>();
        TestWorld(int dimension) {
            super(new net.minecraft.world.storage.SaveHandlerMP(),
                    new net.minecraft.world.storage.WorldInfo(new net.minecraft.world.WorldSettings(0,
                            net.minecraft.world.GameType.CREATIVE, false, false, net.minecraft.world.WorldType.FLAT), "test"),
                    new net.minecraft.world.WorldProviderSurface(), new net.minecraft.profiler.Profiler(), false);
            provider.setDimension(dimension);
        }
        @Override protected net.minecraft.world.chunk.IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }
        @Override public net.minecraft.block.state.IBlockState getBlockState(net.minecraft.util.math.BlockPos pos) {
            net.minecraft.block.state.IBlockState state = blocks.get(pos);
            return state == null ? net.minecraft.init.Blocks.AIR.getDefaultState() : state;
        }
        @Override public boolean setBlockState(net.minecraft.util.math.BlockPos pos,
                                               net.minecraft.block.state.IBlockState state, int flags) {
            blocks.put(pos.toImmutable(), state);
            return true;
        }
    }
}
