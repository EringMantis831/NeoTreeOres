package cn.mcmod.neotreeores.block;

import cn.mcmod.neotreeores.tree.IOreTree;
import cn.mcmod.neotreeores.tree.CtTrees;
import cn.mcmod.neotreeores.NeoTreeOres;
import cn.mcmod.neotreeores.tree.OreTreeGenerators;
import cn.mcmod.neotreeores.tree.OreTreeType;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.common.EnumPlantType;

import java.util.Random;

/**
 * 矿石树树苗。
 *
 * <p>继承原版 {@link BlockBush}（获得"土壤检查 / 无支撑掉落"行为，与原版树苗一致），
 * 实现 {@link IGrowable} 支持骨粉与随机刻自然生长。</p>
 */
public class BlockOreSapling extends BlockBush implements IGrowable {

    private final IOreTree type;

    public BlockOreSapling(IOreTree type) {
        this.type = type;
        this.setHardness(0.0F);
        this.setSoundType(SoundType.PLANT);
        this.setCreativeTab(NeoTreeOres.TAB);
    }

    public IOreTree getTreeType() {
        return type;
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list) {
        list.add(new ItemStack(this));
    }

    /** 与原版树苗一致：平原类植物（决定能种在什么土壤上） */
    @Override
    public EnumPlantType getPlantType(net.minecraft.world.IBlockAccess world, BlockPos pos) {
        return EnumPlantType.Plains;
    }

    // ------------------------------------------------------------------
    // 生长
    // ------------------------------------------------------------------

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (worldIn.isRemote) {
            return;
        }
        // 原版 BlockBush：检查支撑，失去支撑就掉落
        super.updateTick(worldIn, pos, state, rand);

        if (worldIn.getBlockState(pos).getBlock() != this) {
            return;
        }
        if (worldIn.getLightFromNeighbors(pos.up()) >= 9 && rand.nextInt(7) == 0) {
            this.grow(worldIn, rand, pos, state);
        }
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return CtTrees.allowsDimension(type, world.provider.getDimension()) && super.canPlaceBlockAt(world, pos);
    }

    @Override
    public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient) {
        return CtTrees.allowsDimension(type, worldIn.provider.getDimension());
    }

    @Override
    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        return CtTrees.allowsDimension(type, worldIn.provider.getDimension()) && rand.nextFloat() < 0.45F;
    }

    @Override
    public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        if (worldIn.isRemote || worldIn.getBlockState(pos).getBlock() != this) {
            return;
        }
        if (!CtTrees.allowsDimension(type, worldIn.provider.getDimension())) {
            return;
        }
        // 原版 BlockSapling.generateTree 的第一步：发可取消的 TerrainGen 事件，
        // 其它 mod（如植魔/林业）能借此拦掉生长。
        if (!net.minecraftforge.event.terraingen.TerrainGen.saplingGrowTree(worldIn, rand, pos)) {
            return;
        }
        BlockPos origin = pos;
        boolean twoByTwo = false;
        if ("spruce".equals(type.getShape().getTextureBase())) {
            search:
            for (int dx = 0; dx >= -1; --dx) {
                for (int dz = 0; dz >= -1; --dz) {
                    BlockPos corner = pos.add(dx, 0, dz);
                    if (isSameSapling(worldIn, corner) && isSameSapling(worldIn, corner.east())
                            && isSameSapling(worldIn, corner.south())
                            && isSameSapling(worldIn, corner.east().south())) {
                        origin = corner;
                        twoByTwo = true;
                        break search;
                    }
                }
            }
        }
        WorldGenerator generator = OreTreeGenerators.createFor(type, rand, twoByTwo);
        BlockPos[] saplings = twoByTwo
                ? new BlockPos[] {origin, origin.east(), origin.south(), origin.east().south()}
                : new BlockPos[] {pos};
        if (!OreTreeGenerators.canGrowInto(worldIn, origin, generator, saplings)) {
            return;
        }
        IBlockState[] saved = new IBlockState[saplings.length];
        for (int i = 0; i < saplings.length; i++) {
            saved[i] = worldIn.getBlockState(saplings[i]);
            // Match vanilla: suppress neighbor updates while removing the square.
            worldIn.setBlockState(saplings[i], net.minecraft.init.Blocks.AIR.getDefaultState(), 4);
        }
        if (!generator.generate(worldIn, rand, origin)) {
            for (int i = 0; i < saplings.length; i++) {
                worldIn.setBlockState(saplings[i], saved[i], 4);
            }
        }
    }

    private boolean isSameSapling(World world, BlockPos pos) {
        // Registry block identity also distinguishes different CrT mineral species.
        return world.getBlockState(pos).getBlock() == this;
    }

    // ------------------------------------------------------------------
    // 破坏 / 击中粒子：用方块自身贴图 + 满亮度重建（去掉原版 ×0.6 压暗）
    // ------------------------------------------------------------------

    @Override
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public boolean addDestroyEffects(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos,
                                     net.minecraft.client.particle.ParticleManager manager) {
        return cn.mcmod.neotreeores.client.BlockParticles.addDestroyEffects(world, pos, manager);
    }

    @Override
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public boolean addHitEffects(net.minecraft.block.state.IBlockState state, net.minecraft.world.World world,
                                 net.minecraft.util.math.RayTraceResult target,
                                 net.minecraft.client.particle.ParticleManager manager) {
        return cn.mcmod.neotreeores.client.BlockParticles.addHitEffects(state, world, target, manager);
    }


    /** 显示名：内置树取自 lang（中/英），脚本新增树用 OD 名现场拼（资源包可覆盖） */
    @Override
    public String getLocalizedName() {
        return cn.mcmod.neotreeores.tree.TreeNames.sapling(type);
    }
}
