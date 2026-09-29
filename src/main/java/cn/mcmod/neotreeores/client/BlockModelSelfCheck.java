package cn.mcmod.neotreeores.client;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * 进游戏后跑一次的自检：把"静默变黑紫格"的几种情况点名。
 *
 * <p>为什么需要：模型/贴图出问题时 1.12.2 基本不报错，只是 sprite 变成 {@code missingno}
 * 或者整个换成一个缺失模型占位。我们已经被坑过好几次（face 里写绝对路径、
 * {@code particle} 写成同文件里的 {@code "#变量"} 导致**整份模型加载失败**、物品没登记模型……），
 * 所以这里主动查一遍。</p>
 *
 * <p>教训：告警要分类计数，不要混在一起。之前把"方块物品缺 display 变换"和"贴图缺失"
 * 算进同一个 bad，结果 120 件扁平物品的误报把真正缺贴图的 10 件淹掉了。</p>
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public final class BlockModelSelfCheck {

    private static final Logger LOGGER = LogManager.getLogger("neotreeores");
    private static final String MODID = "neotreeores";
    private static boolean done;

    private BlockModelSelfCheck() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getBlockRendererDispatcher() == null || mc.getRenderItem() == null) {
            return;
        }
        done = true;
        try {
            checkItems(mc);
            checkBlocks(mc);
            checkAllStates(mc);
        } catch (Throwable t) {
            LOGGER.warn("[NeoTreeOres] 模型自检本身出错（可忽略）", t);
        }
    }

    private static IBakedModel missingModel(Minecraft mc) {
        return mc.getBlockRendererDispatcher().getBlockModelShapes().getModelManager().getMissingModel();
    }

    /** 物品模型自检 */
    private static void checkItems(Minecraft mc) {
        ItemModelMesher mesher = mc.getRenderItem().getItemModelMesher();
        IBakedModel missing = missingModel(mc);
        // 用来判断"方块物品该有的 GUI 变换"：拿原版石头当基准
        IBakedModel stone = mesher.getItemModel(new ItemStack(Blocks.STONE));
        boolean stoneIsBlockLike = isBlockLike(stone);
        boolean stoneGuiIsCustom = stone != null
                && stone.getItemCameraTransforms().gui != ItemTransformVec3f.DEFAULT;

        int total = 0;
        int missingCount = 0;
        int badSprite = 0;
        int badDisplay = 0;
        int badName = 0;
        int nullFace = 0;

        for (Item item : Item.REGISTRY) {
            if (item.getRegistryName() == null || !MODID.equals(item.getRegistryName().getResourceDomain())) {
                continue;
            }
            total++;
            try {
                ItemStack stack = new ItemStack(item);
                IBakedModel model = mesher.getItemModel(stack);

                // ① 整个模型缺失（黑紫格方块）
                if (model == null || model == missing) {
                    missingCount++;
                    if (missingCount <= 10) {
                        LOGGER.warn("[NeoTreeOres] 物品自检：{} 拿到的是**缺失模型**（模型位置没登记 / 烘焙失败）",
                                item.getRegistryName());
                    }
                    continue;
                }
                // ② 贴图缺失（取真实 quad 的 sprite，物品模型的 particle 不可靠）
                String icon = firstQuadSprite(model);
                if (icon == null || "missingno".equals(icon)) {
                    badSprite++;
                    if (badSprite <= 10) {
                        LOGGER.warn("[NeoTreeOres] 物品自检：{} 的模型贴图缺失（图标会是黑紫格）modelClass={}",
                                item.getRegistryName(), model.getClass().getName());
                    }
                }
                // ③ 方块物品必须有 GUI display 变换（否则物品栏里是"正面朝前的大方块"）
                //    判断方式：方块类模型（没有通用面、有分面 quad）+ gui 变换是默认值 ⇒ 缺 display
                if (stoneIsBlockLike && stoneGuiIsCustom && isBlockLike(model)
                        && model.getItemCameraTransforms().gui == ItemTransformVec3f.DEFAULT) {
                    badDisplay++;
                    if (badDisplay <= 10) {
                        LOGGER.warn("[NeoTreeOres] 物品自检：{} 是方块模型但没有 GUI display 变换", item.getRegistryName());
                    }
                }
                // ④ 六个面 + 通用面都不得返回 null：Forge 渲染附魔光效时会 addAll(getQuads(...))
                String nullOn = firstNullFace(model);
                if (nullOn != null) {
                    nullFace++;
                    if (nullFace <= 10) {
                        LOGGER.warn("[NeoTreeOres] 物品自检：{} 的模型在 {} 上返回 null（会崩渲染）modelClass={}",
                                item.getRegistryName(), nullOn, model.getClass().getName());
                    }
                }
                // ⑤ 显示名不能是本地化 key
                String displayName = stack.getDisplayName();
                if (displayName.startsWith("item.") || displayName.startsWith("tile.")
                        || displayName.endsWith(".name")) {
                    badName++;
                    if (badName <= 10) {
                        LOGGER.warn("[NeoTreeOres] 物品自检：{} 的显示名是本地化 key（{}）", item.getRegistryName(), displayName);
                    }
                }
            } catch (Throwable t) {
                badSprite++;
                if (badSprite <= 10) {
                    LOGGER.warn("[NeoTreeOres] 物品自检：{} 检查时出错: {}", item.getRegistryName(), t.toString());
                }
            }
        }
        summary("物品", total, missingCount, badSprite, badDisplay, badName, nullFace);
    }

    /** 方块模型自检 */
    private static void checkBlocks(Minecraft mc) {
        IBakedModel missing = missingModel(mc);
        int total = 0;
        int missingCount = 0;
        int badParticle = 0;
        int noTint = 0;

        for (Block block : Block.REGISTRY) {
            if (block.getRegistryName() == null || !MODID.equals(block.getRegistryName().getResourceDomain())) {
                continue;
            }
            total++;
            try {
                IBakedModel model = mc.getBlockRendererDispatcher().getBlockModelShapes()
                        .getModelForState(block.getDefaultState());
                if (model == null || model == missing) {
                    missingCount++;
                    if (missingCount <= 10) {
                        LOGGER.warn("[NeoTreeOres] 模型自检：方块 {} 拿到的是**缺失模型**（blockstate/模型加载失败）",
                                block.getRegistryName());
                    }
                    continue;
                }
                TextureAtlasSprite sprite = model.getParticleTexture();
                String icon = sprite == null ? null : sprite.getIconName();
                if (icon == null || "missingno".equals(icon)) {
                    badParticle++;
                    if (badParticle <= 10) {
                        LOGGER.warn("[NeoTreeOres] 模型自检：方块 {} 的 particle 贴图缺失"
                                + "（破坏粒子会是黑紫格；多为模型里 particle 写成同文件 \"#变量\" 导致整份模型加载失败）",
                                block.getRegistryName());
                    }
                }
                // 颜色器必须生效，否则会显示成没上色的灰色基底（tint 没打上去）
                if (mc.getBlockColors().colorMultiplier(block.getDefaultState(), null, null, 0) == -1) {
                    noTint++;
                    if (noTint <= 10) {
                        LOGGER.warn("[NeoTreeOres] 颜色自检：方块 {} 没有 colorizer（会显示成灰色基底）",
                                block.getRegistryName());
                    }
                }
            } catch (Throwable t) {
                badParticle++;
                if (badParticle <= 10) {
                    LOGGER.warn("[NeoTreeOres] 模型自检：方块 {} 检查时出错: {}", block.getRegistryName(), t.toString());
                }
            }
        }
        summary("方块", total, missingCount, badParticle, noTint, 0, 0);
    }

    private static void summary(String kind, int total, int missing, int a, int b, int c, int d) {
        if (missing == 0 && a == 0 && b == 0 && c == 0 && d == 0) {
            LOGGER.info("[NeoTreeOres] {}自检：{} 个模型全部正常", kind, Integer.valueOf(total));
        } else {
            LOGGER.warn("[NeoTreeOres] {}自检：共 {} 个模型 —— 缺失模型 {}、贴图/渲染异常 {}、其它 {}（tint/display/名字）",
                    kind, Integer.valueOf(total), Integer.valueOf(missing), Integer.valueOf(a),
                    Integer.valueOf(b + c + d));
        }
    }

    /**
     * 全部 state 检查：默认状态正常不代表别的状态正常。
     * （曾经踩过的坑：生成的 blockstate 里 variant 键与游戏算出来的属性串不一致，
     * 默认状态恰好是对的，而 {@code axis=x} / {@code decayable=...} 这些全是缺失模型 ——
     * 表现就是横放的原木、开始腐烂的树叶变成黑紫格。）
     */
    private static void checkAllStates(Minecraft mc) {
        IBakedModel missing = missingModel(mc);
        int total = 0;
        int bad = 0;
        for (Block block : Block.REGISTRY) {
            if (block.getRegistryName() == null || !MODID.equals(block.getRegistryName().getResourceDomain())) {
                continue;
            }
            for (IBlockState state : block.getBlockState().getValidStates()) {
                total++;
                try {
                    IBakedModel model = mc.getBlockRendererDispatcher().getBlockModelShapes()
                            .getModelForState(state);
                    if (model == null || model == missing) {
                        bad++;
                        if (bad <= 15) {
                            LOGGER.warn("[NeoTreeOres] 状态自检：{} 拿到缺失模型", state);
                        }
                    }
                } catch (Throwable t) {
                    bad++;
                }
            }
        }
        if (bad == 0) {
            LOGGER.info("[NeoTreeOres] 状态自检：{} 个 state 全部有模型", Integer.valueOf(total));
        } else {
            LOGGER.warn("[NeoTreeOres] 状态自检：{}/{} 个 state 是缺失模型", Integer.valueOf(bad), Integer.valueOf(total));
        }
    }

    /** 方块类模型 = 没有通用面(无朝向 quad) 且至少有一个分面 quad */
    private static boolean isBlockLike(IBakedModel model) {
        if (model == null) {
            return false;
        }
        List<net.minecraft.client.renderer.block.model.BakedQuad> general = model.getQuads(null, null, 0L);
        if (general != null && !general.isEmpty()) {
            return false;
        }
        return !model.getQuads(null, EnumFacing.UP, 0L).isEmpty();
    }

    private static String firstQuadSprite(IBakedModel model) {
        List<net.minecraft.client.renderer.block.model.BakedQuad> quads = model.getQuads(null, null, 0L);
        if (quads == null || quads.isEmpty()) {
            for (EnumFacing facing : EnumFacing.values()) {
                quads = model.getQuads(null, facing, 0L);
                if (quads != null && !quads.isEmpty()) {
                    break;
                }
            }
        }
        if (quads == null || quads.isEmpty()) {
            return null;
        }
        TextureAtlasSprite sprite = quads.get(0).getSprite();
        return sprite == null ? null : sprite.getIconName();
    }

    private static String firstNullFace(IBakedModel model) {
        if (model.getQuads(null, null, 0L) == null) {
            return "通用面";
        }
        for (EnumFacing facing : EnumFacing.values()) {
            if (model.getQuads(null, facing, 0L) == null) {
                return facing.getName();
            }
        }
        return null;
    }
}
