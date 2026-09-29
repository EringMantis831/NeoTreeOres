package cn.mcmod.neotreeores.block;

import cn.mcmod.neotreeores.tree.IOreTree;
import cn.mcmod.neotreeores.NeoTreeOres;
import cn.mcmod.neotreeores.NeoTreeOresConfig;
import cn.mcmod.neotreeores.client.NeoTreeOresParticleType;
import cn.mcmod.neotreeores.tree.OreGate;
import cn.mcmod.neotreeores.tree.OreTreeContent;
import cn.mcmod.neotreeores.tree.OreTreeRegistry;
import cn.mcmod.neotreeores.tree.OreTreeType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Random;

/**
 * 矿石树树叶。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li><b>腐烂走原版</b>：不覆写默认状态，保持 {@code CHECK_DECAY=true} / {@code DECAYABLE=true}，
 *       并完整保留原版 {@code updateTick} 的距离场腐烂算法与 {@code beginLeavesDecay}，
 *       因此与其它 mod（斧头、连锁挖矿、其它树叶）完全兼容。</li>
 *   <li><b>掉落区分来源</b>：原版腐烂最终会调用 {@code getDrops}，为了给"腐烂掉落"和
 *       "玩家破坏掉落"配不同概率，用 {@link #DECAYING} 标记包住 {@code super.updateTick}
 *       （服务端单线程 tick，标记安全）。</li>
 *   <li><b>落叶</b>：随机刻时按概率在地面生成落叶地毯；客户端另有飘落粒子（见 {@code randomDisplayTick}）。
 *       这两者参考 / 移植自 <a href="https://github.com/0999312/Sakura_mod">Sakura</a>
 *       （MIT License, Copyright (c) 2019 0999312，见 {@code LICENSES/MIT.txt}）。</li>
 * </ul>
 */
public class BlockOreLeaves extends BlockLeaves {

    /** 标记当前是否处于"原版腐烂"触发的掉落过程中 */
    private static final ThreadLocal<Boolean> DECAYING = new ThreadLocal<Boolean>();

    private final IOreTree type;

    public BlockOreLeaves(IOreTree type) {
        this.type = type;
        this.setHardness(0.2F);
        this.setResistance(0.2F);
        this.setLightOpacity(1);
        this.setSoundType(SoundType.PLANT);
        this.setCreativeTab(NeoTreeOres.TAB);
        // 注意：不覆写 setDefaultState —— 保持原版 CHECK_DECAY=true / DECAYABLE=true
    }

    public IOreTree getTreeType() {
        return type;
    }

    // ------------------------------------------------------------------
    // 方块状态（仅保留原版两个属性，去掉 variant）
    // ------------------------------------------------------------------

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[] {CHECK_DECAY, DECAYABLE});
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
                .withProperty(DECAYABLE, Boolean.valueOf((meta & 4) == 0))
                .withProperty(CHECK_DECAY, Boolean.valueOf((meta & 8) > 0));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = 0;
        if (!state.getValue(DECAYABLE).booleanValue()) {
            meta |= 4;
        }
        if (state.getValue(CHECK_DECAY).booleanValue()) {
            meta |= 8;
        }
        return meta;
    }

    /** 我们每种树单独一个方块，没有原版 EnumType —— 返回 null（原版允许） */
    @Override
    public BlockPlanks.EnumType getWoodType(int meta) {
        return null;
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list) {
        list.add(new ItemStack(this));
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 0;
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(this);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(this);
    }

    /** 剪刀 → 原版行为：得到方块本身 */
    @Override
    public List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, int fortune) {
        return NonNullList.withSize(1, new ItemStack(this));
    }

    // ------------------------------------------------------------------
    // 掉落表
    // ------------------------------------------------------------------

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        OreTreeContent content = OreTreeRegistry.get(type);
        return content == null ? null : Item.getItemFromBlock(content.getSapling());
    }

    @Override
    protected int getSaplingDropChance(IBlockState state) {
        return NeoTreeOresConfig.of(type).saplingDropChance;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        OreTreeContent content = OreTreeRegistry.get(type);
        if (content == null) {
            return;
        }
        Random rand = world instanceof World ? ((World) world).rand : new Random();
        NeoTreeOresConfig.TreeSettings cfg = NeoTreeOresConfig.of(type);
        boolean decaying = Boolean.TRUE.equals(DECAYING.get());

        int saplingChance = decaying ? cfg.decaySaplingChance : cfg.saplingDropChance;
        int leafDropChance = decaying ? cfg.decayLeafDropChance : cfg.leafDropChance;

        if (rollChance(rand, saplingChance)) {
            drops.add(new ItemStack(content.getSapling()));
        }
        if (rollChance(rand, leafDropChance)) {
            // 时运改的是数量，不是概率（同原版矿物 quantityDroppedWithBonus）
            int count = cn.mcmod.neotreeores.util.FortuneHelper.bonusCount(fortune, rand);
            drops.add(new ItemStack(content.getLeafDrop(), count));
        }
    }

    /** 与原版一致：命中判定就是 1/N，时运不影响概率（只影响上面那个数量） */
    private static boolean rollChance(Random rand, int chance) {
        int c = chance < 1 ? 1 : chance;
        return rand.nextInt(c) == 0;
    }

    // ------------------------------------------------------------------
    // 随机刻：原版腐烂 + 落叶地毯
    // ------------------------------------------------------------------

    /**
     * 玩家手工放下的树叶 = 永久（{@code DECAYABLE=false}）：不会因为旁边放/拆原木而开始腐烂。
     * 自然生成的树叶由树形生成器写成 {@code DECAYABLE=true / CHECK_DECAY=false}，照旧走原版腐烂。
     */
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int meta,
                                            net.minecraft.entity.EntityLivingBase placer,
                                            net.minecraft.util.EnumHand hand) {
        return this.getDefaultState()
                .withProperty(DECAYABLE, Boolean.FALSE)
                .withProperty(CHECK_DECAY, Boolean.FALSE);
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        // 1) 完整走原版腐烂逻辑（被破坏/腐烂的掉落通过 DECAYING 标记区分）
        DECAYING.set(Boolean.TRUE);
        try {
            super.updateTick(worldIn, pos, state, rand);
        } finally {
            DECAYING.remove();
        }

        if (!state.getValue(DECAYABLE)) {
            return;
        }
        // Native leaves have a separate 1/500 random-tick die-off.
        if (worldIn.getBlockState(pos).getBlock() == this && rand.nextInt(500) == 0) {
            worldIn.setBlockToAir(pos);
            if (rand.nextInt(20) == 0) {
                OreTreeContent content = OreTreeRegistry.get(type);
                if (content != null && content.getLeafDrop() != null) {
                    Block.spawnAsEntity(worldIn, pos, new ItemStack(content.getLeafDrop()));
                }
            }
            return;
        }
        // 2) 若方块还在，尝试"落叶"→ 在地面铺落叶地毯
        if (worldIn.isRemote || !NeoTreeOresConfig.fallenLeaves) {
            return;
        }
        if (worldIn.getBlockState(pos).getBlock() != this) {
            return; // 已经腐烂掉了
        }
        if (rand.nextDouble() >= NeoTreeOresConfig.fallenLeavesChance) {
            return;
        }
        trySpawnFallenLeaves(worldIn, pos, rand);
    }

    /** 从树叶位置向下找地面，铺一块落叶地毯 */
    private void trySpawnFallenLeaves(World world, BlockPos pos, Random rand) {
        if (NeoTreeOresConfig.requirePlayerNearby
                && world.getClosestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        NeoTreeOresConfig.playerNearbyRange, false) == null) {
            return;
        }

        OreTreeContent content = OreTreeRegistry.get(type);
        if (content == null) {
            return;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int maxDepth = NeoTreeOresConfig.carpetSearchDepth;
        for (int dy = 1; dy <= maxDepth; dy++) {
            cursor.setPos(pos.getX(), pos.getY() - dy, pos.getZ());
            if (cursor.getY() <= 0) {
                return;
            }
            IBlockState at = world.getBlockState(cursor);
            if (at.getBlock().isAir(at, world, cursor) || at.getBlock().isReplaceable(world, cursor)) {
                BlockPos below = cursor.down();
                IBlockState support = world.getBlockState(below);
                if (validCarpetSupport(world, below, support)) {
                    world.setBlockState(cursor, content.getFallenLeaves().getDefaultState(), 3);
                    return;
                }
            }
        }
    }

    private boolean validCarpetSupport(World world, BlockPos pos, IBlockState state) {
        if (state.getMaterial().isLiquid() || !state.isFullBlock()
                || !state.isSideSolid(world, pos, EnumFacing.UP)) {
            return false;
        }
        IBlockState above = world.getBlockState(pos.up());
        if (above.getMaterial().isLiquid() || above.getBlock() == Blocks.FIRE) {
            return false;
        }
        if (state.getBlock() instanceof net.minecraft.block.BlockBush
                && state.getBlock() != Blocks.TALLGRASS) {
            return false;
        }
        return state.getBlock() != Blocks.GRASS || world.getBlockState(pos.down()).isFullBlock();
    }

    // ------------------------------------------------------------------
    // 客户端落叶粒子
    // ------------------------------------------------------------------

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        // 保留原版行为（雨天树叶滴水）
        Blocks.LEAVES.randomDisplayTick(stateIn, worldIn, pos, rand);

        if (!NeoTreeOresConfig.leafParticles) {
            return;
        }
        if (rand.nextInt(Math.max(1, NeoTreeOresConfig.leafParticleChance)) != 0) {
            return;
        }

        int j = rand.nextInt(2) * 2 - 1;
        int k = rand.nextInt(2) * 2 - 1;

        double x = pos.getX() + 0.5D + 0.25D * j;
        double y = pos.getY() - 0.15D;
        double z = pos.getZ() + 0.5D + 0.25D * k;
        double vx = rand.nextFloat() * j * 0.1D;
        double vy = (rand.nextFloat() * 0.055D) + 0.015D;
        double vz = rand.nextFloat() * k * 0.1D;

        NeoTreeOres.proxy.spawnParticle(NeoTreeOresParticleType.LEAF, type, x, y, z, vx, -vy, vz);
    }

    // ------------------------------------------------------------------
    // 渲染（原版树叶同款）
    // ------------------------------------------------------------------

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        // 原版树叶就是 CUTOUT_MIPPED（透明像素直接挖空 + 使用 mipmap）
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    /**
     * 照抄原版 {@code BlockLeaves.shouldSideBeRendered}，但**用 this 判断**：
     * 之前委托给 {@code Blocks.LEAVES} 时，那份实现里的 {@code this} 是原版树叶，
     * 于是"相邻同种树叶的内侧面"永远剔不掉（快速画质下会看到内部暗面）。
     */
    @SideOnly(Side.CLIENT)
    @Override
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        if (!cn.mcmod.neotreeores.RenderBuildConfig.CUTOUT_TEST
                && !net.minecraft.client.Minecraft.isFancyGraphicsEnabled()) {
            if (world.getBlockState(pos.offset(side)).getBlock() == this) {
                return false;
            }
        }
        return cn.mcmod.neotreeores.RenderBuildConfig.CUTOUT_TEST
                ? true : super.shouldSideBeRendered(state, world, pos, side);
    }

    @Override
    public boolean isLeaves(IBlockState state, IBlockAccess world, BlockPos pos) {
        return true;
    }

    /** 内部用：给其它代码判断该树是否启用 */
    public boolean isEnabled() {
        return OreGate.isEnabled(type);
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
        return cn.mcmod.neotreeores.tree.TreeNames.leaves(type);
    }
}
