package cn.mcmod.neotreeores.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 飘落的树叶粒子（Sakura 同款手感，但做了两处改进）。
 *
 * <p>改进点：</p>
 * <ol>
 *   <li>用 {@code getFXLayer() == 1} —— 由 {@code ParticleManager} 绑定<b>方块图集</b>，
 *       于是可以直接复用该树树叶的贴图（灰度基底 + 套色），不需要额外做粒子图集。</li>
 *   <li>颜色按树种设置（{@code setRBGColorF}），叶子与树本身颜色一致。</li>
 * </ol>
 */
@SideOnly(Side.CLIENT)
public class ParticleFallingLeaf extends Particle {

    public ParticleFallingLeaf(World world, double x, double y, double z,
                               double motionXIn, double motionYIn, double motionZIn,
                               TextureAtlasSprite sprite, int color) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);

        this.setParticleTexture(sprite);

        this.motionX *= 0.1D;
        this.motionY *= 0.1D;
        this.motionZ *= 0.1D;
        this.motionX += motionXIn;
        this.motionY += motionYIn;
        this.motionZ += motionZIn;

        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        this.setRBGColorF(r, g, b);

        this.particleScale = 0.96F + 0.02F * world.rand.nextInt(8);
        this.particleMaxAge = world.rand.nextInt(30) + 120;
        this.particleAlpha = 1.0F;
        this.particleGravity = 0.02F;
        this.canCollide = true;
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
        }

        this.move(this.motionX, this.motionY, this.motionZ);
        this.motionY -= 0.003D;
        this.motionY = Math.max(this.motionY, -0.14D);

        if (this.onGround) {
            this.motionX *= 0.0D;
            this.motionZ *= 0.0D;
        }
    }
}
