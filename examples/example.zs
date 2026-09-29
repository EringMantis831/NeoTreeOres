/*
#添加树种
mods.neotreeores.addOreTree(OreDictName as string)
    .setTreeType(TreeType as string)
    .setTreeColor(TreeColor as int)
    .setLeafColor(LeafColor as int)
    .setRecipeRequired(AmountRequiredToOre as int)
    .setAmountTransfered(AmountTransfer as int)
    .setDimensionRequirement(DimensionList as int[], blacklist as boolean)@Optional
    .setMiningLevel(ToolLevel as int)@Optional
    .setHardness(Hardness as int)Optional
    .build();

#移除树种
mods.neotreeores.remove(OreDictName as string);

#修改已有树种
mods.neotreeores.configOreTree(OreDictName as string)
    .setRecipeRequired(AmountRequiredToOre as int)
    .setAmountTransfered(AmountTransfer as int)
    .setDimensionRequirement(DimensionList as int[], blacklist as boolean)@Optional
    .configure();
    */
mods.neotreeores.addOreTree("Quartz")
    .setTreeType("OAK")
    .setTreeColor(0xAAAAAA)
    .setLeafColor(0xBBBBBB)
    .setRecipeRequired(4)
    .setAmountTransfered(3)
    .setDimensionRequirement([-1], false)/*Optional*/
    .setMiningLevel(1)/*Optional*/
    .setHardness(2)/*Optional*/
    .build();

mods.neotreeores.remove("Diamond");

mods.neotreeores.configOreTree("Emerald")
    .setRecipeRequired(1)
    .setAmountTransfered(64)
    .setDimensionRequirement([-1], true)
    .configure();