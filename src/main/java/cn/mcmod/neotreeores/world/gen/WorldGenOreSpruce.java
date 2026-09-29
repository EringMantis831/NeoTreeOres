package cn.mcmod.neotreeores.world.gen;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraftforge.common.IPlantable;

import java.util.Random;

/**
 * 云杉形矿石树生成器。
 *
 * <p>形状算法照抄原版 {@code WorldGenTaiga2}（高瘦锥形云杉）——因为原版该类把
 * {@code Blocks.LOG}/{@code Blocks.LEAVES} 硬编码死了，没有"传入自定义方块"的入口，
 * 所以本类把它参数化为任意"原木/树叶状态"。</p>
 */
public class WorldGenOreSpruce extends WorldGenAbstractTree {

    private final IBlockState trunk;
    private final IBlockState leaf;

    public WorldGenOreSpruce(boolean notify, IBlockState trunk, IBlockState leaf) {
        super(notify);
        this.trunk = trunk;
        this.leaf = leaf;
    }

    @Override
    public boolean generate(World worldIn, Random rand, BlockPos position) {
        int height = rand.nextInt(4) + 6;
        int bareTrunk = 1 + rand.nextInt(2);
        int leafLayers = height - bareTrunk;
        int maxRadius = 2 + rand.nextInt(2);
        boolean spaceOk = true;

        if (position.getY() >= 1 && position.getY() + height + 1 <= 256) {
            // ---- 空间检查 ----
            for (int y = position.getY(); y <= position.getY() + 1 + height && spaceOk; ++y) {
                int radius;
                if (y - position.getY() < bareTrunk) {
                    radius = 0;
                } else {
                    radius = maxRadius;
                }
                for (int x = position.getX() - radius; x <= position.getX() + radius && spaceOk; ++x) {
                    for (int z = position.getZ() - radius; z <= position.getZ() + radius && spaceOk; ++z) {
                        if (y >= 0 && y < 256) {
                            BlockPos p = new BlockPos(x, y, z);
                            IBlockState s = worldIn.getBlockState(p);
                            if (!cn.mcmod.neotreeores.tree.OreTreeGenerators.isGrowthSpace(worldIn, p)) {
                                spaceOk = false;
                            }
                        } else {
                            spaceOk = false;
                        }
                    }
                }
            }

            if (!spaceOk) {
                return false;
            }

            // ---- 土壤检查 ----
            BlockPos below = position.down();
            IBlockState soil = worldIn.getBlockState(below);
            boolean canSustain = soil.getBlock().canSustainPlant(soil, worldIn, below, EnumFacing.UP, (IPlantable) Blocks.SAPLING);
            if (!canSustain || position.getY() >= 256 - height - 1) {
                return false;
            }
            soil.getBlock().onPlantGrow(soil, worldIn, below, position);

            // ---- 树叶（逐层扩大/收缩的锥形） ----
            int radius = rand.nextInt(2);
            int nextRadius = 1;
            int shrink = 0;
            for (int layer = 0; layer <= leafLayers; ++layer) {
                int y = position.getY() + height - layer;
                for (int x = position.getX() - radius; x <= position.getX() + radius; ++x) {
                    for (int z = position.getZ() - radius; z <= position.getZ() + radius; ++z) {
                        BlockPos p = new BlockPos(x, y, z);
                        IBlockState s = worldIn.getBlockState(p);
                        if (s.getBlock().isAir(s, worldIn, p) || s.getBlock().isLeaves(s, worldIn, p)) {
                            this.setBlockAndNotifyAdequately(worldIn, p, this.leaf);
                        }
                    }
                }
                if (radius >= nextRadius) {
                    radius = shrink;
                    shrink = 1;
                    ++nextRadius;
                    if (nextRadius > maxRadius) {
                        nextRadius = maxRadius;
                    }
                } else {
                    ++radius;
                }
            }

            // ---- 树干 ----
            int trunkOffset = rand.nextInt(3);
            for (int i = 0; i < height - trunkOffset; ++i) {
                BlockPos p = position.up(i);
                IBlockState s = worldIn.getBlockState(p);
                if (s.getBlock().isAir(s, worldIn, p) || s.getBlock().isLeaves(s, worldIn, p)) {
                    this.setBlockAndNotifyAdequately(worldIn, p, this.trunk);
                }
            }
            return true;
        }
        return false;
    }
}
