package cn.mcmod.neotreeores.tree;

import cn.mcmod.neotreeores.integration.crafttweaker.ZenOreTrees;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** No game window or world files: execute pre-scan, then the public API replay. */
public final class CtControlsRegression {
    private static int assertions;
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        String userScript = args.length == 0
                ? "/* mods.neotreeores.remove(\"Iron\"); */\n"
                + "mods.neotreeores.addOreTree(\"Quartz\").setTreeType(\"OAK\").setTreeColor(0xAAAAAA).setLeafColor(0xBBBBBB)"
                + ".setRecipeRequired(4).setAmountTransfered(3).setDimensionRequirement([-1], false).setMiningLevel(1).setHardness(2).build();\n"
                + "mods.neotreeores.remove(\"Diamond\");\n"
                + "mods.neotreeores.configOreTree(\"Emerald\").setRecipeRequired(1).setAmountTransfered(64).setDimensionRequirement([-1], true).configure();"
                : new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        check(CtScriptScanner.scanText("user-script", userScript) == 3, "exact user script accepts 3 statements");
        check(CtTrees.isRemoved(OreTreeType.DIAMOND), "Diamond removed (built-in)");
        check(!OreTreeRegistry.allTrees().contains(OreTreeType.DIAMOND), "Diamond not in registration/integration input");
        check(CtTrees.recipeRequired(OreTreeType.EMERALD) == 1, "Emerald required=1");
        check(CtTrees.recipeYield(OreTreeType.EMERALD) == 64, "Emerald yield=64");
        check(CtTrees.allowsDimension(OreTreeType.EMERALD, 0), "Emerald allowed in overworld");
        check(!CtTrees.allowsDimension(OreTreeType.EMERALD, -1), "Emerald denied in nether");
        IOreTree quartz = CtTrees.findByOre(" Quartz ");
        check(quartz != null, "Quartz was added");
        check(CtTrees.recipeYield(quartz) == 3, "Quartz yield=3 unchanged");
        check(CtTrees.allowsDimension(quartz, -1) && !CtTrees.allowsDimension(quartz, 0), "Quartz whitelist unchanged");

        String regression = "// mods.neotreeores.remove(\"Gold\");\n"
                + "othermod.remove(\"Iron\");\n"
                + "val documentation = \"mods.neotreeores.remove('Iron');\";\n"
                + "mods.neotreeores.addOreTree(\"TempTree\").setRecipeRequired(4).build();\n"
                + "mods.neotreeores.configOreTree(\"TempTree\").setAmountTransfered(9).setDimensionRequirement([-1, 1], false).configure();\n"
                + "mods.neotreeores.remove(\"TempTree\");\n"
                + "mods.neotreeores.configOreTree(\"Emerald\").setAmountTransfered(32).configure();\n"
                + "mods.neotreeores.configOreTree(\"Emerald\").setAmountTransfered(64);";
        check(CtScriptScanner.scanText("regression", regression) == 5, "ordered add/config/remove; foreign calls and comments ignored");
        check(!CtTrees.isRemoved(OreTreeType.IRON) && !CtTrees.isRemoved(OreTreeType.GOLD), "comments/strings/other mods do not remove trees");
        IOreTree temp = CtTrees.findByOre("temptree");
        check(CtTrees.isRemoved(temp), "add followed by remove really removes script tree");
        check(CtTrees.recipeYield(temp) == 9, "configuration follows add in source order");
        check(CtTrees.recipeRequired(OreTreeType.EMERALD) == 1 && !CtTrees.allowsDimension(OreTreeType.EMERALD, -1), "partial setters preserve other fields");
        check(CtTrees.recipeYield(OreTreeType.EMERALD) == 64, "configuration without configure terminal supported");
        check(CtScriptScanner.scanText("bad-array", "mods.neotreeores.configOreTree(\"Iron\").setDimensionRequirement([x, 0], false).configure();") == 0,
                "nonliteral dimensions rejected, not silently converted to zero");
        check(CtTrees.allowsDimension(OreTreeType.IRON, -1), "invalid declaration leaves prior values untouched");
        check(!CtTrees.configurePatch("Emerald", 10, null, null, null), "invalid input count rejected");
        check(CtTrees.recipeRequired(OreTreeType.EMERALD) == 1, "invalid config leaves requirement unchanged");

        CtTrees.freeze();
        check(ZenOreTrees.addOreTree("Quartz").setTreeType("OAK").setTreeColor(0xAAAAAA).setLeafColor(0xBBBBBB)
                .setRecipeRequired(4).setAmountTransfered(3).setDimensionRequirement(new int[]{-1}, false)
                .setMiningLevel(1).setHardness(2).build(), "runtime add replay succeeds");
        ZenOreTrees.remove("Diamond");
        check(ZenOreTrees.configOreTree("Emerald").setRecipeRequired(1).setAmountTransfered(64)
                .setDimensionRequirement(new int[]{-1}, true).configure(), "runtime config replay succeeds");
        check(ZenOreTrees.configOreTree("Emerald").setAmountTransfered(32).configure(), "superseded configuration replay is recognized");
        check(ZenOreTrees.configOreTree("TempTree").setAmountTransfered(9)
                .setDimensionRequirement(new int[]{-1, 1}, false).configure(), "config-before-remove replay does not resurrect tree");
        check(!CtTrees.remove("Iron"), "late unseen removal rejected rather than falsely reporting success");
        check(!CtTrees.configure("Emerald", 8, 2, new int[0], false), "late unseen config rejected, no stale recipes");
        check(CtTrees.recipeYield(OreTreeType.EMERALD) == 64, "replay did not reset pre-scanned configuration");
        System.out.println("PASS CtControlsRegression: " + assertions + " assertions");
    }
}
