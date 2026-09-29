package cn.mcmod.neotreeores;

import cn.mcmod.neotreeores.tree.OreGate;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * NeoTreeOres —— Minecraft 1.12.2 主入口。
 *
 * <p>一个"矿石树"资源 mod：不做任何世界生成，树只能通过种植树苗获得。</p>
 *
 * <p>构建工具链（详见 gradle.properties）：
 * Gradle 4.9 + ForgeGradle 3.0.197 + JDK 1.8.0_501 + Forge 14.23.5.2859。</p>
 */
@Mod(
        modid = NeoTreeOres.MODID,
        name = NeoTreeOres.NAME,
        version = NeoTreeOres.VERSION,
        acceptedMinecraftVersions = NeoTreeOres.ACCEPTED_MC_VERSIONS,
        // 运行期强制 Forge 版本下限：14.23.5.2859
        dependencies = NeoTreeOres.DEPENDENCIES + ";after:bonsaitrees"
)
@Mod.EventBusSubscriber(modid = NeoTreeOres.MODID)
public class NeoTreeOres {

    // ------------------------------------------------------------------
    // 元数据常量（与 gradle.properties / mcmod.info 保持一致）
    // ------------------------------------------------------------------

    /** Mod ID，必须全小写 */
    public static final String MODID = "neotreeores";

    /** Mod 显示名 */
    public static final String NAME = "NeoTreeOres";

    /** Mod 版本，由构建脚本在打包时同步 */
    public static final String VERSION = "1.0.0";

    /** 支持的 Minecraft 版本 */
    public static final String ACCEPTED_MC_VERSIONS = "[1.12.2]";

    /** Forge 最低版本：14.23.5.2859 */
    public static final String FORGE_MIN_VERSION = "14.23.5.2859";

    /** 依赖声明：强制 Forge 不低于 14.23.5.2859 */
    public static final String DEPENDENCIES = "required-after:forge@[" + FORGE_MIN_VERSION + ",)";

    /** 日志器 */
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    /** 静态实例 */
    @Mod.Instance(MODID)
    public static NeoTreeOres instance;

    /** 代理（客户端/服务端） */
    @SidedProxy(
            clientSide = "cn.mcmod.neotreeores.client.ClientProxy",
            serverSide = "cn.mcmod.neotreeores.CommonProxy"
    )
    public static CommonProxy proxy;

    /** 创造模式标签页（内容动态过滤，见 {@link NeoTreeOresTab}） */
    public static final CreativeTabs TAB = new NeoTreeOresTab();

    // ------------------------------------------------------------------
    // 生命周期
    // ------------------------------------------------------------------

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
        // CraftTweaker 的脚本要等 FMLLoadComplete 才执行，而方块/物品必须现在决定，
        // 所以先在 preInit 预读 scripts/ 里的 addOreTree 字面量声明（详见 CtScriptScanner）。
        cn.mcmod.neotreeores.tree.CtScriptScanner.scan(event.getModConfigurationDirectory().getParentFile());
        LOGGER.info("{} preInit (MC 1.12.2, Forge >= {})", NAME, FORGE_MIN_VERSION);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
        cn.mcmod.neotreeores.integration.OptionalMachineRecipes.registerAll();
        cn.mcmod.neotreeores.integration.BonsaiIntegration.registerAll();
    }

    /** 所有 mod 都初始化完成后，才能可靠地检测矿词 → 这里开始缓存 OD 门控结果 */
    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        OreGate.onLoadComplete();
        cn.mcmod.neotreeores.recipe.OreTreeRecipes.registerLogSmelting();
        LOGGER.info("{} OD gate resolved ({} trees enabled)", NAME, countEnabled());
    }

    private static int countEnabled() {
        int n = 0;
        for (cn.mcmod.neotreeores.tree.IOreTree type : cn.mcmod.neotreeores.tree.OreTreeRegistry.allTrees()) {
            if (OreGate.isEnabled(type)) {
                n++;
            }
        }
        return n;
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (MODID.equals(event.getModID())) {
            NeoTreeOresConfig.load();
            OreGate.invalidate();
            OreGate.onLoadComplete();
        }
    }
}
