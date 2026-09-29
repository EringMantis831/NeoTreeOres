package cn.mcmod.neotreeores;

import cn.mcmod.neotreeores.tree.IOreTree;
import cn.mcmod.neotreeores.client.NeoTreeOresParticleType;
import cn.mcmod.neotreeores.tree.OreTreeType;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * 通用代理（服务端/无渲染端）。
 *
 * <p>{@link #spawnParticle} 是空实现 —— 落叶粒子纯客户端，服务端不参与、不发包。</p>
 */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        NeoTreeOresConfig.init(event);
    }

    public void init(FMLInitializationEvent event) {
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    /** 客户端专用：服务端不生成任何粒子 */
    public void spawnParticle(NeoTreeOresParticleType particleType, IOreTree tree,
                              double x, double y, double z,
                              double velX, double velY, double velZ) {
    }
}
