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
