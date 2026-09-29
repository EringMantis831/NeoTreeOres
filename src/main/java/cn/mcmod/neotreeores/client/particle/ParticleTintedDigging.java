package cn.mcmod.neotreeores.client.particle;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 满亮度的挖掘碎屑粒子。
 *
 * <p>原版 {@link ParticleDigging} 是 {@code protected} 构造、并且在构造里把
 * {@code particleRed/Green/Blue} 写死成 {@code 0.6}（碎屑一律压暗 40%）。
 * 继承它就能拿到那个 protected 构造，并在构造后再把颜色改回满亮度，
 * 让碎屑颜色与方块/物品栏颜色一致。</p>
 */
@SideOnly(Side.CLIENT)
public class ParticleTintedDigging extends ParticleDigging {

    public ParticleTintedDigging(World world, double x, double y, double z,
                                 double motionX, double motionY, double motionZ, IBlockState state) {
        super(world, x, y, z, motionX, motionY, motionZ, state);
        this.setRBGColorF(1.0F, 1.0F, 1.0F);    // ← 原版是 0.6
    }
}
