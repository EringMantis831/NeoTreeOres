package cn.mcmod.neotreeores.client;

import cn.mcmod.neotreeores.NeoTreeOres;
import cn.mcmod.neotreeores.tree.CtOreTree;
import cn.mcmod.neotreeores.tree.CtTrees;
import net.minecraft.util.text.translation.LanguageMap;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * 给脚本新增树注入中/英文名。
 *
 * <p>内置 19 棵树的名字写在 lang 文件里；脚本新增树是运行期才知道的，没法写文件，
 * 所以用 Forge 提供的 {@link LanguageMap#inject(java.io.InputStream)} 在每次资源重载后注入一份
 * 内存里的 lang。注入时机选在 {@code TextureStitchEvent.Pre}：资源重载时语言先于贴图加载，
 * 到这里语言表已经就位，且每次重载都会再注入一次（否则会被重载清掉）。</p>
 *
 * <p>名字策略：直接用脚本给的 OD 名 + 部件后缀，例如 {@code Copper} →
 * "Copper Log" / "Copper Wood" / "Copper Leaves" / "Copper Sapling" /
 * "Fallen Copper Leaves" / "Copper Broadleaf"。想换中文名可以自己写资源包覆盖同名 lang key。</p>
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = NeoTreeOres.MODID, value = Side.CLIENT)
public final class CtTreeNames {

    private CtTreeNames() {
    }

    @SubscribeEvent
    public static void onTextureStitchPre(TextureStitchEvent.Pre event) {
        if (CtTrees.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (CtOreTree tree : CtTrees.all()) {
            String name = tree.getOdName();
            append(sb, tree.getLogLangKey(), name + " Log");
            append(sb, tree.getWoodLangKey(), name + " Wood");
            append(sb, tree.getLeavesLangKey(), name + " Leaves");
            append(sb, tree.getSaplingLangKey(), name + " Sapling");
            append(sb, tree.getFallenLeavesLangKey(), "Fallen " + name + " Leaves");
            append(sb, tree.getLeafDropLangKey(),
                    name + " " + (tree.getShape() == cn.mcmod.neotreeores.tree.OreTreeType.Shape.OAK
                            ? "Broadleaf" : "Needleleaf"));
        }
        String content = sb.toString();
        LanguageMap.inject(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        NeoTreeOres.LOGGER.info("[NeoTreeOres] 已注入 {} 棵脚本新增树的名字", Integer.valueOf(CtTrees.size()));
        for (CtOreTree tree : CtTrees.all()) {
            NeoTreeOres.LOGGER.info("[NeoTreeOres] names[{}]: log={}, wood={}, leaves={}, sapling={}, fallen={}, drop={}",
                    new Object[] {tree.getId(),
                            cn.mcmod.neotreeores.tree.TreeNames.log(tree),
                            cn.mcmod.neotreeores.tree.TreeNames.wood(tree),
                            cn.mcmod.neotreeores.tree.TreeNames.leaves(tree),
                            cn.mcmod.neotreeores.tree.TreeNames.sapling(tree),
                            cn.mcmod.neotreeores.tree.TreeNames.fallenLeaves(tree),
                            cn.mcmod.neotreeores.tree.TreeNames.leafDrop(tree)});
        }
    }

    private static void append(StringBuilder sb, String key, String value) {
        // Resource-pack translations take priority over generated English fallbacks.
        if (!net.minecraft.util.text.translation.I18n.canTranslate(key)) {
            sb.append(key).append('=').append(value).append('\n');
        }
    }
}
