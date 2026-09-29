package cn.mcmod.neotreeores.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import cn.mcmod.neotreeores.client.particle.ParticleTintedDigging;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 破坏 / 击中粒子。
 *
 * <p><b>为什么要自己写</b>：原版 {@code ParticleDigging} 在构造里把
 * {@code particleRed/Green/Blue} 写死成 {@code 0.6}，也就是所有方块碎屑都被压暗 40%。
 * 我们的方块材质是"灰度基底 + 颜色"精确生成的，压暗后会明显偏深、与手持/物品栏颜色对不上。</p>
 *
 * <p>做法：覆写 Forge 的 {@code Block.addDestroyEffects / addHitEffects}（都是
 * {@code @SideOnly(CLIENT)}，返回 true 表示"已自行处理，不要再生成原版粒子"），
 * 用**该方块自己的 particle 贴图 + 满亮度**重建粒子，几何分布与速度完全照抄原版，
 * 只是去掉了那 0.6 的压暗。</p>
 */
@SideOnly(Side.CLIENT)
public final class BlockParticles {

    private BlockParticles() {
    }

    /** 破坏方块时的碎屑（原版是 4×4×4 = 64 颗，分布与初速照抄） */
    public static boolean addDestroyEffects(World world, BlockPos pos, ParticleManager manager) {
        IBlockState state = world.getBlockState(pos);
        TextureAtlasSprite sprite = spriteFor(state);
        int count = 0;
        for (int j = 0; j < 4; ++j) {
            for (int k = 0; k < 4; ++k) {
                for (int l = 0; l < 4; ++l) {
                    double x = pos.getX() + (j + 0.5D) / 4.0D;
                    double y = pos.getY() + (k + 0.5D) / 4.0D;
                    double z = pos.getZ() + (l + 0.5D) / 4.0D;
                    double vx = x - (pos.getX() + 0.5D);
                    double vy = y - (pos.getY() + 0.5D);
                    double vz = z - (pos.getZ() + 0.5D);
                    ParticleTintedDigging particle = new ParticleTintedDigging(world, x, y, z, vx, vy, vz, state);
                    particle.setBlockPos(pos);
                    applyTint(particle, state, world, pos);
                    if (sprite != null) {
                        particle.setParticleTexture(sprite);
                    }
                    manager.addEffect(particle);
                    count++;
                }
            }
        }
        return count > 0;
    }

    /** 挖掘进度变化时的碎屑（照抄原版 addBlockHitEffects 的位置/速度，去掉压暗） */
    public static boolean addHitEffects(IBlockState state, World world, RayTraceResult target, ParticleManager manager) {
        BlockPos pos = target.getBlockPos();
        if (pos == null || state.getMaterial() == net.minecraft.block.material.Material.AIR) {
            return false;
        }
        TextureAtlasSprite sprite = spriteFor(state);
        EnumFacing side = target.sideHit;
        double x = pos.getX() + world.rand.nextDouble();
        double y = pos.getY() + world.rand.nextDouble();
        double z = pos.getZ() + world.rand.nextDouble();
        float inset = 0.1F;
        switch (side) {
            case DOWN:
                y = pos.getY() + inset;
                break;
            case UP:
                y = pos.getY() + 1.0D - inset;
                break;
            case NORTH:
                z = pos.getZ() + inset;
                break;
            case SOUTH:
                z = pos.getZ() + 1.0D - inset;
                break;
            case WEST:
                x = pos.getX() + inset;
                break;
            case EAST:
                x = pos.getX() + 1.0D - inset;
                break;
            default:
                break;
        }
        ParticleTintedDigging particle = new ParticleTintedDigging(world, x, y, z, 0.0D, 0.0D, 0.0D, state);
        particle.setBlockPos(pos);
        applyTint(particle, state, world, pos);
        if (sprite != null) {
            particle.setParticleTexture(sprite);
        }
        particle.multiplyVelocity(0.2F);
        particle.multipleParticleScaleBy(0.6F);
        manager.addEffect(particle);
        return true;
    }

    /** 取该方块模型里 "particle" 指定的贴图（我们在模型里已指向方块自己的材质） */
    private static void applyTint(ParticleTintedDigging particle, IBlockState state, World world, BlockPos pos) {
        int color = Minecraft.getMinecraft().getBlockColors().colorMultiplier(state, world, pos, 0);
        particle.setRBGColorF(((color >> 16) & 255) / 255.0F,
                ((color >> 8) & 255) / 255.0F, (color & 255) / 255.0F);
    }

    private static TextureAtlasSprite spriteFor(IBlockState state) {
        try {
            return Minecraft.getMinecraft().getBlockRendererDispatcher()
                    .getBlockModelShapes().getTexture(state);
        } catch (Throwable t) {
            return null;
        }
    }
}
