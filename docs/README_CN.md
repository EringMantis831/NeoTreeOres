# NeoTreeOres

**简体中文** | [English](README.md)

把矿物资源搬到树上的 Minecraft 模组：种下矿树苗，长成后砍原木、剁树叶，再合成回对应的矿物。
支持用 CraftTweaker 脚本自己加树、删树、改树。

- 目标环境：Minecraft **1.12.2** + Forge **14.23.5.2859** 或更高
- 19 种内置矿树：煤炭 / 铁 / 金 / 钻石 / 绿宝石 / 红石 / 青金石 / 铜 / 银 / 铅 / 锡 /
  红宝石 / 蓝宝石 / 铂金 / 镍 / 铱 / 钛 / 铀 / 霓虹
- 每棵树一整套：原木、六面树皮木、树叶、树苗、落叶地毯、树叶掉落物
- 树叶腐烂走原版 `check_decay` / `decayable`，与其它模组兼容
- 可选联动：BonsaiTrees 盆栽、Mekanism CEU 有机种植机、Thermal Expansion 有机灌注机，均带 JEI 展示
- 材质用「共用灰度基底 + tint」：19 棵树共用 10 份方块模型，颜色由 `IBlockColor` / `IItemColor` 在运行期给出

## CraftTweaker

脚本放在游戏目录的 `scripts/` 下（与 `config/` 同级），重启生效：

```zenscript
mods.neotreeores.addOreTree("Quartz")
    .setTreeType("OAK")          // OAK / SPRUCE
    .setTreeColor(0xAAAAAA)      // 树干颜色 → 原木 + 木头方块
    .setLeafColor(0xBBBBBB)      // 树叶颜色 → 树叶 + 树苗 + 地毯
    .setRecipeRequired(4)        // 几个落叶合成一次
    .setAmountTransfered(3)      // 一次产出几个
    .setDimensionRequirement([-1], false)   // 可选，维度白/黑名单
    .build();

mods.neotreeores.remove("Diamond");                          // 删掉一棵树
mods.neotreeores.configOreTree("Emerald")                    // 改已有树
    .setRecipeRequired(1).setAmountTransfered(64).configure();
```

完整示例见 [`examples/example.zs`](../examples/example.zs)。

注意两点：

1. 参数请**直接写字面量**。本模组会在 preInit 预读脚本里声明的树，变量、循环、函数算出来的值读不到。
2. 启动时按矿词自动判断一棵树是否启用（依次试 `ore` / `ingot` / `gem` / `dust` / `nugget` /
   `block` / `plate` / `gear` / `raw` + 树种名，最后试裸名）。矿词由别的模组注册，
   因此有没有装对应矿物 mod 会直接影响哪些树生效。

## 许可证

NeoTreeOres 以 [MIT License](../LICENSE) 发布。第三方许可证原文放在 [`LICENSES/`](../LICENSES)：
`MIT.txt` 是落叶机制所参考的 Sakura 的许可证，`LGPL-2.1.txt` 覆盖 Minecraft Forge / Forge
Mod Loader——本模组只针对它编译，不随包分发它的代码。

## 来源与致谢

- [Sakura](https://github.com/0999312/Sakura_mod)：本模组的**落叶地毯**与**飘落树叶粒子**
  参考 / 移植自它（MIT License, Copyright (c) 2019 0999312，全文见
  [`LICENSES/MIT.txt`](../LICENSES/MIT.txt)）。
- **TreeOres**（作者 Dima Kevanishvili AKA Lessoner）：矿树这个点子的来源。它的 README 写明
  「You can not take any code from my project」，`LICENSE.md` 只有一行版权声明、没有授权使用权，
  所以**本项目没有引用它的任何代码**；树种与配比数据来自我们自己整理的数据表。
- Minecraft Forge / MCP：见 [`CREDITS.txt`](../CREDITS.txt)。
