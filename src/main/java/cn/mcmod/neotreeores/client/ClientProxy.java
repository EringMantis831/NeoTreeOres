package cn.mcmod.neotreeores.client;

import cn.mcmod.neotreeores.tree.IOreTree;
import cn.mcmod.neotreeores.CommonProxy;
import cn.mcmod.neotreeores.NeoTreeOresConfig;
import cn.mcmod.neotreeores.client.particle.ParticleFallingLeaf;
import cn.mcmod.neotreeores.tree.OreTreeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 客户端代理。
 *
 * <p><b>关于渲染</b>：本 mod 的材质由 {@code tools/TextureTinter.java} 在构建期
 * 用「灰度基底 + 每棵树的颜色」生成（木材 multiply、树叶 normalize），
 * 已经是彩色贴图，因此：</p>
 * <ul>
 *   <li>不需要 {@code IBlockColor}/{@code ItemColors}（不需要 tintindex）</li>
 *   <li>不需要自定义 state mapper / 物品模型注册（每棵树都有同名 blockstates + models/item）</li>
 *   <li>破坏粒子、小地图、JEI 等"直接读贴图"的地方颜色天然正确</li>
 * </ul>
 *
 * <p>所以客户端这边只剩一件事：落叶粒子。</p>
 */
@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    private static final String BLOCK_TEX = "neotreeores:blocks/";
    private static final String ITEM_TEX = "neotreeores:items/";

    @Override
    public void spawnParticle(NeoTreeOresParticleType particleType, IOreTree tree,
                              double x, double y, double z,
                              double velX, double velY, double velZ) {
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        if (world == null || mc.effectRenderer == null || tree == null) {
            return;
        }

        // 尊重玩家的"粒子效果"设置（Sakura 这里有一处死代码，我们修正它）
        if (NeoTreeOresConfig.respectParticleSetting) {
            int setting = mc.gameSettings.particleSetting;
            if (setting == 2 || (setting == 1 && world.rand.nextInt(3) == 0)) {
                return;
            }
        }

        // Shared grayscale sprites are stitched by the generated item models.
        String shape = tree.getShape().getTextureBase();
        String leaf = "spruce".equals(shape) ? "base_needleleaf" : "base_broadleaf";
        TextureAtlasSprite sprite = mc.getTextureMapBlocks().getAtlasSprite(ITEM_TEX + leaf);
        if (sprite == mc.getTextureMapBlocks().getMissingSprite()) {
            sprite = mc.getTextureMapBlocks().getAtlasSprite(BLOCK_TEX + "base_leaves_" + shape);
        }
        if (sprite == null || sprite == mc.getTextureMapBlocks().getMissingSprite()) {
            return;
        }

        int color = NeoTreeOresConfig.of(tree).foliageColor & 0xFFFFFF;
        Particle particle = new ParticleFallingLeaf(world, x, y, z, velX, velY, velZ, sprite, color);
        mc.effectRenderer.addEffect(particle);
    }
}
