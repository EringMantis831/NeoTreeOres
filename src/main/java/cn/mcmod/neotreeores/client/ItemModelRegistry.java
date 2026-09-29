package cn.mcmod.neotreeores.client;

import cn.mcmod.neotreeores.NeoTreeOres;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 物品模型登记。
 *
 * <p><b>为什么必须显式登记</b>：1.12.2 里物品不会自动按注册名去找 {@code models/item/<名字>.json}，
 * 只有被 {@link ModelLoader#setCustomModelResourceLocation} 登记过的物品才会去烘焙模型。
 * 没登记的物品一律拿到 {@code ModelLoader$VanillaModelWrapper$1}（缺失模型占位），
 * 表现就是**图标和手持全是黑紫格**——而且游戏不会报任何错。</p>
 *
 * <p>这里统一遍历本 mod 的所有已注册物品（内置 19 棵树 + 脚本新增的动态树都覆盖），
 * 登记为 {@code <注册名>#inventory}，于是每个物品都会去读 {@code models/item/<注册名>.json}。</p>
 *
 * <p><b>脚本新增树必须在同一个方法里先处理</b>（{@link ScriptTreeModels#bind()}）：
 * 脚本树没有随包的 item 模型文件，登记成 {@code <注册名>#inventory} 就是指向一个不存在的文件
 * （黑紫格）。两个 @Mod.EventBusSubscriber 之间谁先跑没保证，分开写就会互相覆盖。</p>
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public final class ItemModelRegistry {

    private ItemModelRegistry() {
    }

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        // 先接脚本树（返回它处理过的物品，下面跳过，避免被覆盖成不存在的模型位置）
        java.util.Set<Item> scriptTrees = ScriptTreeModels.bind();

        int n = 0;
        int skipped = 0;
        for (Item item : Item.REGISTRY) {
            ResourceLocation rl = item.getRegistryName();
            if (rl == null || !NeoTreeOres.MODID.equals(rl.getResourceDomain())) {
                continue;
            }
            if (scriptTrees.contains(item)) {
                skipped++;
                continue;
            }
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(rl, "inventory"));
            n++;
        }
        org.apache.logging.log4j.LogManager.getLogger("neotreeores")
                .info("[NeoTreeOres] 已登记 {} 个物品模型（<注册名>#inventory），脚本树 {} 件走别名",
                        Integer.valueOf(n), Integer.valueOf(skipped));

    }
}
