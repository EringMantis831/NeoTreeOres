package cn.mcmod.neotreeores.tree;

import cn.mcmod.neotreeores.block.BlockFallenLeaves;
import cn.mcmod.neotreeores.block.BlockOreLeaves;
import cn.mcmod.neotreeores.block.BlockOreLog;
import cn.mcmod.neotreeores.block.BlockOreSapling;
import cn.mcmod.neotreeores.item.ItemOreLeafDrop;

/**
 * 一棵矿石树所拥有的全部方块/物品实例。
 */
public final class OreTreeContent {

    private final IOreTree type;

    private BlockOreLog log;
    private BlockOreLog wood;
    private BlockOreLeaves leaves;
    private BlockOreSapling sapling;
    private BlockFallenLeaves fallenLeaves;
    private ItemOreLeafDrop leafDrop;

    public OreTreeContent(IOreTree type) {
        this.type = type;
    }

    public IOreTree getType() {
        return type;
    }

    public BlockOreLog getLog() {
        return log;
    }

    public void setLog(BlockOreLog log) {
        this.log = log;
    }

    public BlockOreLog getWood() {
        return wood;
    }

    public void setWood(BlockOreLog wood) {
        this.wood = wood;
    }

    public BlockOreLeaves getLeaves() {
        return leaves;
    }

    public void setLeaves(BlockOreLeaves leaves) {
        this.leaves = leaves;
    }

    public BlockOreSapling getSapling() {
        return sapling;
    }

    public void setSapling(BlockOreSapling sapling) {
        this.sapling = sapling;
    }

    public BlockFallenLeaves getFallenLeaves() {
        return fallenLeaves;
    }

    public void setFallenLeaves(BlockFallenLeaves fallenLeaves) {
        this.fallenLeaves = fallenLeaves;
    }

    public ItemOreLeafDrop getLeafDrop() {
        return leafDrop;
    }

    public void setLeafDrop(ItemOreLeafDrop leafDrop) {
        this.leafDrop = leafDrop;
    }
}
