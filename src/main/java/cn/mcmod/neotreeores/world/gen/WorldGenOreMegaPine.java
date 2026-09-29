package cn.mcmod.neotreeores.world.gen;

import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenMegaPineTree;

/** Reuses both vanilla mega spruce/pine crowns, height and growth checks. */
public final class WorldGenOreMegaPine extends WorldGenMegaPineTree {
    private final IBlockState mineralLog;
    private final IBlockState mineralLeaves;

    public WorldGenOreMegaPine(boolean tallCrown, IBlockState log, IBlockState leaves) {
        super(false, tallCrown);
        this.mineralLog = log;
        this.mineralLeaves = leaves;
    }

    @Override
    public boolean isReplaceable(World world, BlockPos pos) {
        return cn.mcmod.neotreeores.tree.OreTreeGenerators.isGrowthSpace(world, pos);
    }

    @Override
    protected void setBlockAndNotifyAdequately(World world, BlockPos pos, IBlockState state) {
        if (state.getBlock() == Blocks.LOG) {
            state = mineralLog.withProperty(BlockLog.LOG_AXIS, state.getValue(BlockLog.LOG_AXIS));
        } else if (state.getBlock() == Blocks.LEAVES) {
            state = mineralLeaves;
        }
        super.setBlockAndNotifyAdequately(world, pos, state);
    }
}
