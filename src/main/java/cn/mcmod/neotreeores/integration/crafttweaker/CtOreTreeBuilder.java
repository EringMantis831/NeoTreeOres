package cn.mcmod.neotreeores.integration.crafttweaker;

import cn.mcmod.neotreeores.tree.CtTrees;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/** ZenScript fluent builder for runtime-declared ore trees. */
@ZenClass("mods.neotreeoresbuilder")
@ZenRegister
public final class CtOreTreeBuilder {
    private final String ore;
    private String shape = "OAK";
    private int logColor = 0x8A8A8A;
    private int leafColor = 0x7FBF5F;
    private int required = 0;
    private int transferred = 1;
    private int[] dimensions = new int[0];
    private boolean blacklist = false;
    private int miningLevel = 1;
    private int hardness = 2;
    private boolean requiredSet, transferredSet, dimensionsSet;
    private final boolean configMode;

    public CtOreTreeBuilder(String ore) { this(ore, false); }
    public CtOreTreeBuilder(String ore, boolean configMode) { this.ore = ore; this.configMode = configMode; }
    @ZenMethod public CtOreTreeBuilder setTreeType(String value) { shape = value; return this; }
    @ZenMethod public CtOreTreeBuilder setTreeColor(int value) { logColor = value; return this; }
    @ZenMethod public CtOreTreeBuilder setLeafColor(int value) { leafColor = value; return this; }
    @ZenMethod public CtOreTreeBuilder setRecipeRequired(int value) {
        required = value; requiredSet = true;
        if (configMode) CtTrees.configurePatch(ore, value, null, null, null);
        return this;
    }
    @ZenMethod public CtOreTreeBuilder setAmountTransfered(int value) {
        transferred = value; transferredSet = true;
        if (configMode) CtTrees.configurePatch(ore, null, value, null, null);
        return this;
    }
    @ZenMethod public CtOreTreeBuilder setDimensionRequirement(int[] value, boolean valueBlacklist) {
        dimensions = value == null ? new int[0] : value.clone(); blacklist = valueBlacklist; dimensionsSet = true;
        if (configMode) CtTrees.configurePatch(ore, null, null, dimensions, blacklist);
        return this;
    }
    @ZenMethod public CtOreTreeBuilder setMiningLevel(int value) { miningLevel = value; return this; }
    @ZenMethod public CtOreTreeBuilder setHardness(int value) { hardness = value; return this; }
    @ZenMethod public boolean build() {
        return CtTrees.add(ore, logColor, leafColor, shape, required, transferred, miningLevel, hardness, dimensions, blacklist);
    }

    @ZenMethod public boolean configure() {
        return CtTrees.configurePatch(ore, requiredSet ? required : null, transferredSet ? transferred : null,
                dimensionsSet ? dimensions : null, dimensionsSet ? blacklist : null);
    }
}
