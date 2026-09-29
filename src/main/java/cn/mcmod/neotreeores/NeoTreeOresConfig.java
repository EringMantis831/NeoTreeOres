package cn.mcmod.neotreeores;

import cn.mcmod.neotreeores.tree.IOreTree;
import cn.mcmod.neotreeores.tree.OreTreeType;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 配置文件（1.12.2 用原版 {@link Configuration}，因为每棵树要动态生成一组配置项）。
 *
 * <p>结构：</p>
 * <pre>
 * general {
 *     leafParticles, leafParticleChance, respectParticleSetting,
 *     fallenLeaves, fallenLeavesChance, carpetSearchDepth, requirePlayerNearby,
 *     playerNearbyRange, carpetFortuneBonus, registerGenericOreDict
 * }
 * trees.iron { enabled, leafDropChance, saplingDropChance,
 *              decaySaplingChance, decayLeafDropChance, logColor, foliageColor }
 * 颜色走原版 tint（IBlockColor/IItemColor），两个构建变体行为一致
 * </pre>
 */
public final class NeoTreeOresConfig {

    private static final String CAT_GENERAL = "general";
    private static final String CAT_TREES = "trees";

    // ---- general ----
    public static boolean leafParticles = true;
    public static int leafParticleChance = 40;
    public static boolean respectParticleSetting = true;

    public static boolean fallenLeaves = true;
    public static double fallenLeavesChance = 0.05D;
    public static int carpetSearchDepth = 8;
    public static boolean requirePlayerNearby = true;
    public static int playerNearbyRange = 32;
    public static int carpetFortuneBonus = 1;

    public static boolean registerGenericOreDict = true;

    // ---- per tree ----
    /** 键是树的 id：内置枚举与脚本新增树共用一张表 */
    private static final Map<String, TreeSettings> TREE_SETTINGS = new HashMap<String, TreeSettings>();

    private static Configuration configuration;

    private NeoTreeOresConfig() {
    }

    /** 每棵树的独立设置 */
    public static final class TreeSettings {
        public String enabled = "auto";
        public int leafDropChance = 20;
        public int saplingDropChance = 20;
        public int decaySaplingChance = 20;
        public int decayLeafDropChance = 200;
        /** 树干颜色（套在 log 灰度基底上；仅 colored 变体生效） */
        public int logColor;
        /** 树叶颜色（套在 leaves/sapling 灰度基底上；仅 colored 变体生效） */
        public int foliageColor;

        TreeSettings(IOreTree type) {
            this.logColor = type.getDefaultLogColor();
            this.foliageColor = type.getDefaultFoliageColor();
        }
    }

    public static TreeSettings of(IOreTree type) {
        if (type == null) {
            return new TreeSettings(BUILTIN_FALLBACK);
        }
        TreeSettings s = TREE_SETTINGS.get(type.getId());
        if (s == null) {
            s = new TreeSettings(type);
            TREE_SETTINGS.put(type.getId(), s);
        }
        return s;
    }

    /** 仅在传入 null 时兜底，避免调用方 NPE */
    private static final IOreTree BUILTIN_FALLBACK = OreTreeType.COAL;

    public static void init(FMLPreInitializationEvent event) {
        File file = new File(event.getModConfigurationDirectory(), "neotreeores.cfg");
        configuration = new Configuration(file);
        load();
    }

    /** 供 ConfigChangedEvent 调用 */
    public static void load() {
        if (configuration == null) {
            return;
        }
        configuration.load();

        leafParticles = configuration.getBoolean("leafParticles", CAT_GENERAL, true,
                "树叶是否持续产生客户端飘落的落叶粒子");
        leafParticleChance = configuration.getInt("leafParticleChance", CAT_GENERAL, 40, 1, 10000,
                "落叶粒子触发概率 1/N（仅客户端）");
        respectParticleSetting = configuration.getBoolean("respectParticleSetting", CAT_GENERAL, true,
                "是否尊重玩家的\"粒子效果\"设置（关闭时即使调到最少也照常显示）");

        fallenLeaves = configuration.getBoolean("fallenLeaves", CAT_GENERAL, true,
                "树叶随机刻时是否在地面生成落叶地毯");
        fallenLeavesChance = configuration.get(CAT_GENERAL, "fallenLeavesChance", 0.05D,
                "每次随机刻生成落叶地毯的概率（0.0 ~ 1.0）").setMinValue(0.0D).setMaxValue(1.0D).getDouble();
        carpetSearchDepth = configuration.getInt("carpetSearchDepth", CAT_GENERAL, 8, 1, 32,
                "向下搜索可放置落叶地毯的最大距离");
        requirePlayerNearby = configuration.getBoolean("requirePlayerNearby", CAT_GENERAL, true,
                "仅在有玩家附近时才生成落叶地毯（性能友好）");
        playerNearbyRange = configuration.getInt("playerNearbyRange", CAT_GENERAL, 32, 4, 128,
                "上一条的玩家检测半径");
        carpetFortuneBonus = configuration.getInt("carpetFortuneBonus", CAT_GENERAL, 1, 0, 8,
                "破坏落叶地毯时，把“原版矿物式时运加成”的额外部分放大几倍（0 = 时运无效，1 = 原版行为）");

        registerGenericOreDict = configuration.getBoolean("registerGenericOreDict", CAT_GENERAL, true,
                "额外注册通用矿词 logWood / treeLeaves / treeSapling，提升跨 mod 兼容性");

        // ---- 每棵树 ----
        for (OreTreeType type : OreTreeType.values()) {
            TreeSettings s = of(type);
            String cat = CAT_TREES + "." + type.getId();

            s.enabled = configuration.getString("enabled", cat, "auto",
                    "是否启用该树：auto = 按矿物 OD 自动判定，true = 强制启用，false = 强制关闭")
                    .trim().toLowerCase(Locale.ROOT);
            s.leafDropChance = configuration.getInt("leafDropChance", cat, 20, 1, 100000,
                    "破坏树叶时掉落" + type.getLeafDropId() + "的概率 1/N（时运只加数量，不改概率）");
            s.saplingDropChance = configuration.getInt("saplingDropChance", cat, 20, 1, 100000,
                    "破坏树叶时掉落树苗的概率 1/N（原版橡树为 20，不受时运影响）");
            s.decaySaplingChance = configuration.getInt("decaySaplingChance", cat, 20, 1, 100000,
                    "树叶腐烂时掉落树苗的概率 1/N（原版橡树为 20）");
            s.decayLeafDropChance = configuration.getInt("decayLeafDropChance", cat, 200, 1, 100000,
                    "树叶腐烂时掉落" + type.getLeafDropId() + "的概率 1/N（原版橡树的苹果位为 200）");

            // 颜色：树干与树叶分开指定（十六进制 RRGGBB）
            // 仅 colored 变体生效（方块材质在客户端运行期由“灰度基底 + 该色值”生成）；
            // baked 变体请改 tools/neotreeores-textures.json 并重跑 tools/gen-textures 与 gen-models。
            s.logColor = parseColor(configuration.getString("logColor", cat, hex(type.getDefaultLogColor()),
                    "树干颜色（原版 tint：套在 " + type.getShape().getTextureBase() + " 原木灰度基底上，改完下次渲染即生效）"),
                    type.getDefaultLogColor());
            s.foliageColor = parseColor(configuration.getString("foliageColor", cat, hex(type.getDefaultFoliageColor()),
                    "树叶颜色（原版 tint：套在树叶/树苗灰度基底上，改完下次渲染即生效）"),
                    type.getDefaultFoliageColor());
        }

        // 说明：没有运行期重载配置的指令（重启生效，减少 bug）。

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    private static String hex(int color) {
        return String.format(Locale.ROOT, "%06X", color & 0xFFFFFF);
    }

    private static int parseColor(String raw, int fallback) {
        try {
            String s = raw.trim().replace("#", "").replace("0x", "").replace("0X", "");
            return (int) (Long.parseLong(s, 16) & 0xFFFFFFL);
        } catch (Exception e) {
            return fallback;
        }
    }
}
