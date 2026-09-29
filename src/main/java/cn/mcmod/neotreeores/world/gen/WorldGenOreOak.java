package cn.mcmod.neotreeores.world.gen;

import cn.mcmod.neotreeores.tree.OreTreeGenerators;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenTrees;

/** Vanilla oak geometry, without treating existing wood as an empty growth space. */
public final class WorldGenOreOak extends WorldGenTrees {
    public WorldGenOreOak(IBlockState log, IBlockState leaves) {
        super(true, 4, log, leaves, false);
    }

    @Override
    public boolean isReplaceable(World world, BlockPos pos) {
        return OreTreeGenerators.isGrowthSpace(world, pos);
    }
}
