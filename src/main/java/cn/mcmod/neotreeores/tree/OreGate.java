package cn.mcmod.neotreeores.tree;

import cn.mcmod.neotreeores.NeoTreeOresConfig;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.oredict.OreDictionary;

import java.util.HashMap;
import java.util.Map;

/**
 * OD 门控：判断某棵树是否"启用"。
 *
 * <p>规则（用户授权由实现方决定）—— 按优先级命中任一矿词即视为检测到：</p>
 * <pre>ore&lt;OD&gt; → ingot&lt;OD&gt; → gem&lt;OD&gt; → dust&lt;OD&gt; → nugget&lt;OD&gt; → block&lt;OD&gt;
 * → plate&lt;OD&gt; → gear&lt;OD&gt; → raw&lt;OD&gt; → &lt;OD&gt;（裸名）</pre>
 *
 * <p>配置项 {@code enabled} 为三态：{@code auto}（默认，按 OD）/ {@code true} / {@code false}。</p>
 *
 * <p>注意：矿词由其它 mod 在各自 preInit/init 阶段注册，所以这里<b>惰性求值 + 缓存</b>，
 * 并且在 {@link FMLLoadCompleteEvent} 之后才开始缓存（此前每次现算，避免过早锁死结果）。</p>
 *
 * <p>内置树与脚本新增树共用这一套逻辑，缓存键用树的 id（枚举与配置树可以混用）。</p>
 */
public final class OreGate {

    private static final String[] PREFIXES = {
            "ore", "ingot", "gem", "dust", "nugget", "block", "plate", "gear", "raw"
    };

    private static final Map<String, Boolean> CACHE = new HashMap<String, Boolean>();

    /** 所有 mod 加载完成后置为 true，之后才启用缓存 */
    private static volatile boolean loaded;

    private OreGate() {
    }

    public static boolean isEnabled(IOreTree type) {
        if (type == null || CtTrees.isRemoved(type)) {
            return false;
        }
        String mode = NeoTreeOresConfig.of(type).enabled;
        if ("true".equalsIgnoreCase(mode)) {
            return true;
        }
        if ("false".equalsIgnoreCase(mode)) {
            return false;
        }
        if (!loaded) {
            return detect(type);
        }
        Boolean cached = CACHE.get(type.getId());
        if (cached == null) {
            cached = Boolean.valueOf(detect(type));
            CACHE.put(type.getId(), cached);
        }
        return cached.booleanValue();
    }

    /** 是否是"自动判定"模式 */
    public static boolean isAuto(IOreTree type) {
        return "auto".equalsIgnoreCase(NeoTreeOresConfig.of(type).enabled);
    }

    /** 是否需要注册该树的方块/物品：只有被显式关掉的“脚本新增树”不注册（内置树永远注册，保证存档 ID 稳定） */
    public static boolean shouldRegister(IOreTree type) {
        if (type == null || CtTrees.isRemoved(type)) return false;
        if (type instanceof CtOreTree) {
            return !"false".equalsIgnoreCase(NeoTreeOresConfig.of(type).enabled);
        }
        return type != null;
    }

    private static boolean detect(IOreTree type) {
        if (type.isAlwaysEnabled()) {
            return true;
        }
        String od = type.getOdName();
        for (String prefix : PREFIXES) {
            if (hasLiveOre(prefix + od)) {
                return true;
            }
        }
        return hasLiveOre(od);
    }

    /** Names can survive removal of all their entries; only live stacks enable a tree. */
    private static boolean hasLiveOre(String name) {
        for (net.minecraft.item.ItemStack stack : OreDictionary.getOres(name, false)) {
            if (!stack.isEmpty()) return true;
        }
        return false;
    }

    /** 由主类在 FMLLoadCompleteEvent 调用：清缓存并开始缓存 */
    public static void onLoadComplete() {
        CACHE.clear();
        for (IOreTree type : OreTreeRegistry.allTrees()) {
            CACHE.put(type.getId(), Boolean.valueOf(detect(type)));
        }
        loaded = true;
    }

    /** 供调试/重载使用 */
    public static void invalidate() {
        CACHE.clear();
        loaded = false;
    }
}
