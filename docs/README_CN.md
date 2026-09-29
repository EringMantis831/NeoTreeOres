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

## 安装

把 `neotreeores-1.12.2-1.0.0.jar` 放进 `mods/`。所有联动都是软依赖，装不装都能启动。

配方默认是「树叶 → 矿物」，数量按树种不同（例如煤炭树 3 落叶 → 1 煤炭、铁树 5 → 1 铁锭）。
另外还有「4 原木 → 3 木头」和「8 矿物块 + 任意树苗 → 该树树苗」。

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

## 配置

`config/neotreeores.cfg`：一组通用项（落叶地毯概率、粒子开关、生成地毯时是否要求玩家在附近等），
加上每棵树一组选项（`enabled` 三态、四种掉落概率、两个颜色）。

`enabled` 是 `auto`（默认，按矿词判断）/ `true` / `false`。设成 `false` 会让对应的**脚本新增树**
不注册；内置 19 棵永远注册，以免破坏已有存档的 ID 映射。

## 从源码构建

需要 **JDK 8**（ForgeGradle 3 + Gradle 4.9 不支持更高版本），先把 `JAVA_HOME` 指到 JDK 8，再用
Gradle wrapper：

```bash
./gradlew build          # 首次会下载约 200MB
./gradlew runClient      # 开发客户端
```

构建前请把 `jei_1.12.2-4.22.0.1035.jar` 放进仓库的 `libs/` 目录。JEI 插件要对它的 API 编译，
而这个版本不在 JEI 官方 maven 上（那边最高只有 4.16.1.302），所以这个 jar 不随仓库分发。

产物在 `build/libs/neotreeores-1.12.2-1.0.0-dev2.jar`。注意 `gradle.properties` 里的
`org.gradle.java.home` 是一台机器上的固定路径，换机器要改。首次构建需联网下载
MC / MCP / Forge userdev，通常 5~20 分钟。

### 回归测试

无窗口、不建存档，可以直接跑：

```bash
gradlew verifyCtControls verifyCtRegistries
# 用 CraftTweaker 自带编译器实编译执行脚本，需要指定实例里的两个 jar
gradlew verifyCtZen -PctRegressionScript=<脚本路径> -PctCompilerJar=<CraftTweaker jar>
```

## 目录结构

```
├── LICENSE                                  # 本模组的 MIT 许可证
├── LICENSES/                                # MIT.txt（Sakura）、LGPL-2.1.txt（Forge）
├── docs/                                    # README.md（英文）与 README_CN.md（中文）
├── src/main/java/cn/mcmod/neotreeores/
│   ├── block/ client/ item/ recipe/ util/   # 方块、客户端渲染、物品、配方
│   ├── tree/                                # 树种定义、注册、门控、生成器、CrT 解析
│   ├── world/gen/                           # 橡树 / 大橡树 / 云杉 / 巨型松杉
│   └── integration/                         # BonsaiTrees / Mekanism / Thermal / JEI / CrT
├── src/main/resources/assets/neotreeores/   # 模型、blockstate、lang、贴图
├── src/test/                                # 上述无窗口回归
└── examples/example.zs                      # CraftTweaker 示例
```

## 常见问题

**构建卡在下载或超时？** 需要能访问 `maven.minecraftforge.net`、`libraries.minecraft.net`、
`piston-data.mojang.com`、`services.gradle.org`。走代理的话自己设 `GRADLE_OPTS`。

**报 `Could not determine java version from '26.x'`？** 用 JDK 26 启动了 Gradle，把 `JAVA_HOME`
指到 JDK 8。

**`runClient` 报 `NetworkRegistry.newChannel` 的 NPE？** 这是 ForgeGradle 合并工具注入的
`Side.BUKKIT` 常量导致的，修正已内置在 `build.gradle` 里，来龙去脉写在那个文件顶部的注释中。
若仍出现，说明缓存里还留着旧的合并产物，把下面两个目录删掉再构建：

```
%USERPROFILE%\.gradle\caches\forge_gradle\mcp_repo
%USERPROFILE%\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.12.2-14.23.5.2859*
```

**为什么 jar 名字带 `dev2`？** 历史遗留的构建产物名，功能上就是 1.0.0，发布时用的文件名是
`neotreeores-1.12.2-1.0.0.jar`。

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
