package cn.mcmod.neotreeores.world.gen;

import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenBigTree;

/** Vanilla big oak geometry and space checks, with this tree's mineral states. */
public final class WorldGenOreFancyOak extends WorldGenBigTree {
    private final IBlockState log;
    private final IBlockState leaves;

    public WorldGenOreFancyOak(boolean notify, IBlockState log, IBlockState leaves) {
        super(notify);
        this.log = log;
        this.leaves = leaves;
    }

    @Override
    public boolean isReplaceable(World world, BlockPos pos) {
        return cn.mcmod.neotreeores.tree.OreTreeGenerators.isGrowthSpace(world, pos);
    }

    @Override
    protected void setBlockAndNotifyAdequately(World world, BlockPos pos, IBlockState state) {
        if (state.getBlock() == Blocks.LOG) {
            state = log.withProperty(BlockLog.LOG_AXIS, state.getValue(BlockLog.LOG_AXIS));
        } else if (state.getBlock() == Blocks.LEAVES) {
            state = leaves;
        }
        super.setBlockAndNotifyAdequately(world, pos, state);
    }
}
