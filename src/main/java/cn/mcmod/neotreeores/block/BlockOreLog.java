package cn.mcmod.neotreeores.block;

import cn.mcmod.neotreeores.tree.IOreTree;
import cn.mcmod.neotreeores.tree.OreTreeType;
import net.minecraft.block.BlockLog;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * 矿石树原木 / 木头。
 *
 * <p>直接继承原版 {@link BlockLog}，因此自动获得：</p>
 * <ul>
 *   <li>原版"破坏原木 → 周围树叶 {@code beginLeavesDecay}"的联动（兼容其它 mod 的原版腐烂机制）</li>
 *   <li>{@code canSustainLeaves} / {@code isWood} 语义（其它 mod 与本 mod 的树叶都靠它判定）</li>
 *   <li>原版 LOG_AXIS 朝向逻辑（竖放/横放）</li>
 * </ul>
 *
 * <p>颜色不单独画材质，而是由客户端 {@code IBlockColor} 对橡木/云杉基底贴图套色实现。</p>
 */
public class BlockOreLog extends BlockLog {

    private final IOreTree type;
    private final boolean allBark;

    /**
     * @param type    所属树种
     * @param allBark true = "木头"（六面树皮，装饰用）；false = 普通原木（顶面年轮）
     */
    public BlockOreLog(IOreTree type, boolean allBark) {
        this.type = type;
        this.allBark = allBark;
        // 硬度/挖掘等级由树决定：内置树 = 2.0 / 0（与原来一致）；脚本树由 addOreTree 的 @Optional 参数给出
        this.setHardness(type.getHardness());
        this.setHarvestLevel("axe", type.getToolLevel());
        this.setResistance(2.0F);
        this.setSoundType(net.minecraft.block.SoundType.WOOD);
        this.setDefaultState(this.blockState.getBaseState().withProperty(LOG_AXIS, EnumAxis.Y));
    }

    public IOreTree getTreeType() {
        return type;
    }

    /** true = 六面树皮的"木头"方块 */
    public boolean isAllBark() {
        return allBark;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[] {LOG_AXIS});
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        IBlockState state = this.getDefaultState();
        switch (meta & 12) {
            case 4:
                state = state.withProperty(LOG_AXIS, EnumAxis.X);
                break;
            case 8:
                state = state.withProperty(LOG_AXIS, EnumAxis.Z);
                break;
            case 12:
                state = state.withProperty(LOG_AXIS, EnumAxis.NONE);
                break;
            default:
                state = state.withProperty(LOG_AXIS, EnumAxis.Y);
                break;
        }
        return state;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = 0;
        switch (state.getValue(LOG_AXIS)) {
            case X:
                meta |= 4;
                break;
            case Z:
                meta |= 8;
                break;
            case NONE:
                meta |= 12;
                break;
            default:
                break;
        }
        return meta;
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
    public boolean canSustainLeaves(IBlockState state, IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isWood(IBlockAccess world, BlockPos pos) {
        return true;
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
        return allBark ? cn.mcmod.neotreeores.tree.TreeNames.wood(type)
                : cn.mcmod.neotreeores.tree.TreeNames.log(type);
    }
}
