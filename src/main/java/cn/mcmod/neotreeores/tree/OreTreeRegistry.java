package cn.mcmod.neotreeores.tree;

import cn.mcmod.neotreeores.NeoTreeOres;
import cn.mcmod.neotreeores.NeoTreeOresConfig;
import cn.mcmod.neotreeores.block.BlockFallenLeaves;
import cn.mcmod.neotreeores.block.BlockOreLeaves;
import cn.mcmod.neotreeores.block.BlockOreLog;
import cn.mcmod.neotreeores.block.BlockOreSapling;
import cn.mcmod.neotreeores.item.ItemBlockOreTree;
import cn.mcmod.neotreeores.item.ItemOreLeafDrop;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一注册所有矿石树的方块/物品/矿词。
 *
 * <p>策略：<b>方块与物品永远注册</b>（避免注册顺序、空引用与存档 ID 问题），
 * "是否启用"只体现在创造标签页可见性与配方（门控见 {@link OreGate}）。</p>
 */
@Mod.EventBusSubscriber(modid = NeoTreeOres.MODID)
public final class OreTreeRegistry {

    /** 键是树的 id（内置枚举与脚本新增树共用一张表） */
    private static final Map<String, OreTreeContent> CONTENT = new LinkedHashMap<String, OreTreeContent>();

    /** 注意：不要缓存 —— 脚本可能在注册事件之前才声明完，缓存会让脚本树漏注册 */

    private OreTreeRegistry() {
    }

    /** 全部树（内置 + 配置新增），注册/门控/贴图生成共用 */
    public static List<IOreTree> allTrees() {
        List<IOreTree> list = new ArrayList<IOreTree>();
        for (OreTreeType type : OreTreeType.values()) {
            if (!CtTrees.isRemoved(type)) list.add(type);
        }
        list.addAll(CtTrees.all());
        return Collections.unmodifiableList(list);
    }

    public static OreTreeContent get(IOreTree type) {
        return type == null ? null : CONTENT.get(type.getId());
    }

    public static Collection<OreTreeContent> all() {
        return Collections.unmodifiableCollection(CONTENT.values());
    }

    // ------------------------------------------------------------------
    // 方块
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        CtTrees.freeze();
        for (IOreTree type : allTrees()) {
            if (!OreGate.shouldRegister(type)) {
                continue;
            }
            OreTreeContent content = new OreTreeContent(type);

            BlockOreLog log = new BlockOreLog(type, false);
            log.setRegistryName(NeoTreeOres.MODID, type.getLogId());
            log.setUnlocalizedName(NeoTreeOres.MODID + "." + type.getLogId());
            event.getRegistry().register(log);
            content.setLog(log);

            BlockOreLog wood = new BlockOreLog(type, true);
            wood.setRegistryName(NeoTreeOres.MODID, type.getWoodId());
            wood.setUnlocalizedName(NeoTreeOres.MODID + "." + type.getWoodId());
            event.getRegistry().register(wood);
            content.setWood(wood);

            BlockOreLeaves leaves = new BlockOreLeaves(type);
            leaves.setRegistryName(NeoTreeOres.MODID, type.getLeavesId());
            leaves.setUnlocalizedName(NeoTreeOres.MODID + "." + type.getLeavesId());
            event.getRegistry().register(leaves);
            content.setLeaves(leaves);

            BlockOreSapling sapling = new BlockOreSapling(type);
            sapling.setRegistryName(NeoTreeOres.MODID, type.getSaplingId());
            sapling.setUnlocalizedName(NeoTreeOres.MODID + "." + type.getSaplingId());
            event.getRegistry().register(sapling);
            content.setSapling(sapling);

            BlockFallenLeaves fallen = new BlockFallenLeaves(type);
            fallen.setRegistryName(NeoTreeOres.MODID, type.getFallenLeavesId());
            fallen.setUnlocalizedName(NeoTreeOres.MODID + "." + type.getFallenLeavesId());
            event.getRegistry().register(fallen);
            content.setFallenLeaves(fallen);

            // Logs, leaves and carpets follow vanilla wooden-tree fire behavior.
            Blocks.FIRE.setFireInfo(log, 5, 5);
            Blocks.FIRE.setFireInfo(leaves, 30, 60);
            Blocks.FIRE.setFireInfo(fallen, 30, 60);

            CONTENT.put(type.getId(), content);
        }
    }

    // ------------------------------------------------------------------
    // 物品（含 ItemBlock）与矿词
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (OreTreeContent content : CONTENT.values()) {
            IOreTree type = content.getType();

            registerItemBlock(event, content.getLog(), type.getLogId());
            registerItemBlock(event, content.getWood(), type.getWoodId());
            registerItemBlock(event, content.getLeaves(), type.getLeavesId());
            registerItemBlock(event, content.getSapling(), type.getSaplingId());
            registerItemBlock(event, content.getFallenLeaves(), type.getFallenLeavesId());

            ItemOreLeafDrop leafDrop = new ItemOreLeafDrop(type);
            leafDrop.setRegistryName(NeoTreeOres.MODID, type.getLeafDropId());
            leafDrop.setUnlocalizedName(NeoTreeOres.MODID + "." + type.getLeafDropId());
            event.getRegistry().register(leafDrop);
            content.setLeafDrop(leafDrop);
        }

        // 矿词必须在物品注册之后
        for (OreTreeContent content : CONTENT.values()) {
            IOreTree type = content.getType();

            // 用户指定的命名规则
            OreDictionary.registerOre(type.getSaplingOreDict(), new net.minecraft.item.ItemStack(content.getSapling()));
            OreDictionary.registerOre(type.getLeafDropOreDict(), new net.minecraft.item.ItemStack(content.getLeafDrop()));

            // 通用矿词（跨 mod 兼容）
            if (NeoTreeOresConfig.registerGenericOreDict) {
                OreDictionary.registerOre("treeSapling", new net.minecraft.item.ItemStack(content.getSapling()));
                OreDictionary.registerOre("treeLeaves", new net.minecraft.item.ItemStack(content.getLeaves()));
                OreDictionary.registerOre("logWood", new net.minecraft.item.ItemStack(content.getLog()));
            }
        }
    }

    private static void registerItemBlock(RegistryEvent.Register<Item> event, Block block, String name) {
        ItemBlock item = new ItemBlockOreTree(block);
        item.setRegistryName(NeoTreeOres.MODID, name);
        item.setUnlocalizedName(NeoTreeOres.MODID + "." + name);
        event.getRegistry().register(item);
    }
}
