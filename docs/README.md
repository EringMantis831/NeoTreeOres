# NeoTreeOres

[简体中文](README_CN.md) | **English**

A Minecraft mod that puts mineral resources on trees: plant an ore sapling, let it grow, chop the
logs and leaves, then craft them back into the corresponding ore. Trees can also be added, removed
and reconfigured from CraftTweaker scripts.

- Targets Minecraft **1.12.2** with Forge **14.23.5.2859** or newer
- 19 built-in ore trees: coal / iron / gold / diamond / emerald / redstone / lapis / copper /
  silver / lead / tin / ruby / sapphire / platinum / nickel / iridium / titanium / uranium / neon
- Each tree ships a full set: log, all-bark wood, leaves, sapling, fallen-leaves carpet, leaf drop
- Leaves still decay through vanilla `check_decay` / `decayable`, so other mods stay compatible
- Optional integration: BonsaiTrees pots, Mekanism CEU Organic Farm, Thermal Expansion
  Phytogenic Insolator, all with JEI display
- Textures use a shared grayscale base plus tint: all 19 trees share 10 block models, and colours
  come from `IBlockColor` / `IItemColor` at runtime

## Installation

Drop `neotreeores-1.12.2-1.0.0.jar` into `mods/`. Every integration is a soft dependency, so the mod
starts with or without them.

The default recipe converts leaves into ore, and the ratio depends on the species (for example
3 coal leaves to 1 coal, 5 iron leaves to 1 iron ingot). There is also `4 logs -> 3 wood` and
`8 mineral blocks + any sapling -> that tree's sapling`.

## CraftTweaker

Put scripts in the game directory's `scripts/` folder (next to `config/`) and restart:

```zenscript
mods.neotreeores.addOreTree("Quartz")
    .setTreeType("OAK")          // OAK / SPRUCE
    .setTreeColor(0xAAAAAA)      // trunk colour -> log + wood blocks
    .setLeafColor(0xBBBBBB)      // foliage colour -> leaves + sapling + carpet
    .setRecipeRequired(4)        // leaves per craft
    .setAmountTransfered(3)      // output per craft
    .setDimensionRequirement([-1], false)   // optional, dimension allow/deny list
    .build();

mods.neotreeores.remove("Diamond");                          // delete a tree
mods.neotreeores.configOreTree("Emerald")                    // edit an existing one
    .setRecipeRequired(1).setAmountTransfered(64).configure();
```

See [`examples/example.zs`](../examples/example.zs) for a complete example.

Two things to keep in mind:

1. Write arguments as **plain literals**. This mod pre-reads the tree declarations during preInit,
   so values produced by variables, loops or function calls cannot be picked up.
2. At startup each tree is enabled based on ore dictionary lookups (it tries `ore` / `ingot` / `gem` /
   `dust` / `nugget` / `block` / `plate` / `gear` / `raw` + the ore name, then the bare name). Those
   entries are registered by other mods, so which mineral mods you have installed decides which
   trees become active.

## Configuration

`config/neotreeores.cfg` holds one general group (fallen-leaves chance, particle toggle, whether a
player must be nearby, and so on) plus one group per tree (the `enabled` tri-state, four drop
chances, two colours).

`enabled` accepts `auto` (default, decided by ore dictionary), `true` or `false`. Setting it to
`false` stops a **script-added** tree from being registered; the 19 built-in trees are always
registered so existing saves keep their ID mappings.

## Building from source

You need **JDK 8** (ForgeGradle 3 with Gradle 4.9 does not work on newer versions). Point `JAVA_HOME`
at your JDK 8 and use the Gradle wrapper:

```bash
./gradlew build          # the first run downloads about 200MB
./gradlew runClient      # development client
```

Before building, drop `jei_1.12.2-4.22.0.1035.jar` into the `libs/` folder. The JEI plugin compiles
against that API, and the version is not on JEI's public maven (which stops at 4.16.1.302), so the jar
is not distributed with this repository.

Output lands in `build/libs/neotreeores-1.12.2-1.0.0-dev2.jar`. Note that `org.gradle.java.home` in
`gradle.properties` is a machine-specific path and has to be changed elsewhere. The first build needs
network access to download Minecraft, MCP and the Forge userdev jars and usually takes 5 to 20
minutes.

### Regression tests

Headless, no save directory is created:

```bash
gradlew verifyCtControls verifyCtRegistries
# compiles and runs a script with CraftTweaker's own compiler; needs two jars from your instance
gradlew verifyCtZen -PctRegressionScript=<script path> -PctCompilerJar=<CraftTweaker jar>
```

## Repository layout

```
├── LICENSE                                  # MIT License (this mod)
├── LICENSES/                                # MIT.txt (Sakura), LGPL-2.1.txt (Forge)
├── docs/                                    # README.md (English) and README_CN.md (Chinese)
├── src/main/java/cn/mcmod/neotreeores/
│   ├── block/ client/ item/ recipe/ util/   # blocks, client rendering, items, recipes
│   ├── tree/                                # tree definitions, registry, gating, generators, CrT parsing
│   ├── world/gen/                           # oak / big oak / spruce / mega pine-spruce
│   └── integration/                         # BonsaiTrees / Mekanism / Thermal / JEI / CrT
├── src/main/resources/assets/neotreeores/   # models, blockstates, lang, textures
├── src/test/                                # the headless regression tests above
└── examples/example.zs                      # CraftTweaker example
```

## Troubleshooting

**Build hangs while downloading or times out?** It needs `maven.minecraftforge.net`,
`libraries.minecraft.net`, `piston-data.mojang.com` and `services.gradle.org`. Behind a proxy, set
`GRADLE_OPTS` in your shell.

**`Could not determine java version from '26.x'`?** Gradle was started with a newer JDK. Point
`JAVA_HOME` at JDK 8.

**`runClient` crashes with an NPE in `NetworkRegistry.newChannel`?** That comes from a `Side.BUKKIT`
constant injected by ForgeGradle's merge tool; the fix is built into `build.gradle`, and the
reasoning is in the comment block at the top of that file. If it still happens, an old merged jar is
still cached — delete these two directories and rebuild:

```
%USERPROFILE%\.gradle\caches\forge_gradle\mcp_repo
%USERPROFILE%\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.12.2-14.23.5.2859*
```

**Why does the jar name contain `dev2`?** A leftover build artifact name. This is functionally
1.0.0; the file used for releases is `neotreeores-1.12.2-1.0.0.jar`.

## License

NeoTreeOres is released under the [MIT License](../LICENSE). Third-party license texts live in
[`LICENSES/`](../LICENSES): `MIT.txt` is the licence of the Sakura code that the fallen-leaves
mechanic was adapted from, and `LGPL-2.1.txt` covers Minecraft Forge / Forge Mod Loader, which
this mod builds against but does not redistribute.

## Credits and provenance

- [Sakura](https://github.com/0999312/Sakura_mod): the **fallen-leaves carpet** and the **falling
  leaf particles** are adapted from it (MIT License, Copyright (c) 2019 0999312; full text in
  [`LICENSES/MIT.txt`](../LICENSES/MIT.txt)).
- **TreeOres** by Dima Kevanishvili AKA Lessoner: the origin of the "mineral trees" idea. Its README
  states "You can not take any code from my project" and its `LICENSE.md` carries only a copyright
  line with no grant, so **this project uses none of its code**; the species list and recipe ratios
  come from a data table we maintain ourselves.
- Minecraft Forge / MCP: see [`CREDITS.txt`](../CREDITS.txt).
