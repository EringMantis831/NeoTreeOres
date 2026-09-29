package cn.mcmod.neotreeores.tree;

import cn.mcmod.neotreeores.block.BlockOreLeaves;
import cn.mcmod.neotreeores.block.BlockOreLog;
import cn.mcmod.neotreeores.world.gen.WorldGenOreFancyOak;
import cn.mcmod.neotreeores.world.gen.WorldGenOreMegaPine;
import cn.mcmod.neotreeores.world.gen.WorldGenOreOak;
import cn.mcmod.neotreeores.world.gen.WorldGenOreSpruce;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

/** Creates mineral trees with the original oak/mega-pine geometry. */
public final class OreTreeGenerators {
    private OreTreeGenerators() {}

    /** Existing wood and liquids are obstacles even though vanilla accepts wood in its space check. */
    public static boolean isGrowthSpace(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (state.getMaterial().isLiquid() || block.isWood(world, pos)) return false;
        return block.isAir(state, world, pos) || block.isLeaves(state, world, pos)
                || state.getMaterial().isReplaceable();
    }

    /**
     * Check the full maximum canopy envelope before touching saplings. Vanilla big oak
     * checks only its trunk line, while mega pine permits existing wood in its volume.
     * A generously bounded canopy check keeps their random branches/crowns from
     * growing through player blocks; the saplings to be consumed are exempted.
     */
    public static boolean canGrowInto(World world, BlockPos origin, WorldGenerator generator, BlockPos[] saplings) {
        boolean mega = generator instanceof WorldGenOreMegaPine;
        boolean fancy = generator instanceof WorldGenOreFancyOak;
        boolean spruce = generator instanceof WorldGenOreSpruce;
        // Mega pine: base 13 + nextInt(3) + nextInt(15), plus the generator's top check.
        int height = mega ? 31 : fancy ? 20 : spruce ? 11 : 8;
        for (int dy = 0; dy <= height; dy++) {
            int min = dy == 0 ? 0 : (mega ? -7 : fancy ? -8 : spruce ? -3 : -2);
            int max = dy == 0 ? (mega ? 1 : 0) : (mega ? 8 : fancy ? 8 : spruce ? 3 : 2);
            if (spruce && dy == 1) { min = 0; max = 0; }
            for (int dx = min; dx <= max; dx++) {
                for (int dz = min; dz <= max; dz++) {
                    BlockPos p = origin.add(dx, dy, dz);
                    if (p.getY() < 0 || p.getY() >= world.getHeight()) return false;
                    boolean ownSapling = false;
                    if (dy == 0) for (BlockPos sapling : saplings) {
                        if (p.equals(sapling)) { ownSapling = true; break; }
                    }
                    if (!ownSapling && !isGrowthSpace(world, p)) return false;
                }
            }
        }
        return true;
    }

    public static WorldGenerator createFor(IOreTree type, Random random) {
        return createFor(type, random, false);
    }

    public static WorldGenerator createFor(IOreTree type, Random random, boolean twoByTwo) {
        OreTreeContent content = OreTreeRegistry.get(type);
        if (content == null) throw new IllegalStateException("Ore tree content not registered: " + type);
        BlockOreLog log = content.getLog();
        BlockOreLeaves leaves = content.getLeaves();
        IBlockState logState = log.getDefaultState();
        IBlockState leafState = leaves.getDefaultState()
                .withProperty(BlockOreLeaves.CHECK_DECAY, Boolean.FALSE)
                .withProperty(BlockOreLeaves.DECAYABLE, Boolean.TRUE);

        // Vanilla makes the 1/10 big-oak draw before species selection.
        boolean fancyOak = random.nextInt(10) == 0;
        switch (type.getShape()) {
            case SPRUCE:
                if (twoByTwo) return new WorldGenOreMegaPine(random.nextBoolean(), logState, leafState);
                return new WorldGenOreSpruce(true, logState, leafState);
            case OAK:
            default:
                if (fancyOak) return new WorldGenOreFancyOak(true, logState, leafState);
                return new WorldGenOreOak(logState, leafState);
        }
    }
}
