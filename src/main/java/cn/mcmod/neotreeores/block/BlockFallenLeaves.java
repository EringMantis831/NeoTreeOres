package cn.mcmod.neotreeores.block;

import cn.mcmod.neotreeores.tree.IOreTree;
import cn.mcmod.neotreeores.NeoTreeOres;
import cn.mcmod.neotreeores.NeoTreeOresConfig;
import cn.mcmod.neotreeores.tree.OreTreeContent;
import cn.mcmod.neotreeores.tree.OreTreeRegistry;
import cn.mcmod.neotreeores.tree.OreTreeType;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import java.util.Random;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 落叶地毯 —— 1/8 高、无碰撞的装饰性地毯方块（参考 Sakura 的 {@code BlockFallenLeaves}）。
 *
 * <p>由树叶的随机刻自动铺到地面；被破坏时必定掉落对应物品（针叶/阔叶），并受时运影响。</p>
 */
public class BlockFallenLeaves extends Block {

    protected static final AxisAlignedBB CARPET_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D);

    private final IOreTree type;

    public BlockFallenLeaves(IOreTree type) {
        // Snow uses a non-blocking material so BlockLiquid can flow into and replace it.
        super(Material.SNOW);
        this.type = type;
        this.setHardness(0.1F);
        this.setResistance(0.1F);
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

    // ------------------------------------------------------------------
    // 形状
    // ------------------------------------------------------------------

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return CARPET_AABB;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        if (side == EnumFacing.UP) {
            return true;
        }
        return blockAccess.getBlockState(pos.offset(side)).getBlock() == this
                || super.shouldSideBeRendered(blockState, blockAccess, pos, side);
    }

    /** 允许"落叶"直接覆盖草等可替换方块 */
    @Override
    public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos) {
        // Match a one-layer snow cover: flowing liquid may replace this block in-place.
        return true;
    }

    // ------------------------------------------------------------------
    // 支撑检查（与原版地毯一致）
    // ------------------------------------------------------------------

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        return super.canPlaceBlockAt(worldIn, pos) && canBlockStay(worldIn, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!canBlockStay(worldIn, pos)) {
            this.dropBlockAsItem(worldIn, pos, state, 0);
            worldIn.setBlockToAir(pos);
        }
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        // Like vanilla snow: liquid flow replaces this non-blocking material in-place;
        // do not delete the carpet merely because water is adjacent.
        if (!canBlockStay(worldIn, pos)) {
            this.dropBlockAsItem(worldIn, pos, state, 0);
            worldIn.setBlockToAir(pos);
        }
    }

    private boolean canBlockStay(World worldIn, BlockPos pos) {
        IBlockState support = worldIn.getBlockState(pos.down());
        if (support.getMaterial().isLiquid() || !support.isFullBlock()
                || !support.isSideSolid(worldIn, pos.down(), EnumFacing.UP)) {
            return false;
        }
        IBlockState above = worldIn.getBlockState(pos);
        if (above.getMaterial().isLiquid() || above.getBlock() == Blocks.FIRE) {
            return false;
        }
        if (support.getBlock() instanceof net.minecraft.block.BlockBush
                && support.getBlock() != Blocks.TALLGRASS) {
            return false;
        }
        return support.getBlock() != Blocks.GRASS
                || worldIn.getBlockState(pos.down(2)).isFullBlock();
    }

    // ------------------------------------------------------------------
    // 掉落：必定掉落对应物品，受时运影响
    // ------------------------------------------------------------------

    @Override
    public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
        // Material.SNOW lets flowing water replace this block, but normally requires
        // a shovel; carpet must still drop its leaf item when broken by hand.
        return true;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        OreTreeContent content = OreTreeRegistry.get(type);
        if (content == null) {
            return;
        }
        // 时运：照抄原版矿物的数量加成（概率不受影响，地毯本来就是必掉）
        java.util.Random rand = world instanceof World ? ((World) world).rand : new java.util.Random();
        int count = cn.mcmod.neotreeores.util.FortuneHelper.bonusCount(
                fortune, rand, NeoTreeOresConfig.carpetFortuneBonus);
        if (count < 1) {
            count = 1;
        }
        drops.add(new ItemStack(content.getLeafDrop(), count));
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 0;
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
        return cn.mcmod.neotreeores.tree.TreeNames.fallenLeaves(type);
    }
}
