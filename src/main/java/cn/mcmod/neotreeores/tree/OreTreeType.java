package cn.mcmod.neotreeores.tree;

/**
 * 矿石树元数据表 —— 唯一数据源，由 {@code task.xls} 固化而来。
 *
 * <p>列对应关系：矿物(中文树名) | 矿物名(id 前缀) | 矿物OD | 树形 | 英文名 | 检测矿词？ | 落叶消耗量 | 矿物产出量</p>
 *
 * <p>命名规则（用户指定）：</p>
 * <ul>
 *   <li>{@code <ore>_sapling} → 矿词 {@code sapling<OD>}</li>
 *   <li>{@code <ore>_needleleaf}（云杉形）/ {@code <ore>_broadleaf}（橡树形）→ 矿词 {@code needleleaf<OD>} / {@code broadleaf<OD>}</li>
 * </ul>
 */
public enum OreTreeType implements IOreTree {

    //                 id          OD         中文名       英文名          树形          门控          原木色      树叶色      落叶消耗量 矿物产出量
    COAL      ("coal",      "Coal",      "碳化树",   "Charcoal",     Shape.OAK,    Gate.ALWAYS, 0x2B2B2B, 0x3A3A3A,  3,  1),
    IRON      ("iron",      "Iron",      "铁树",     "Cycas",        Shape.OAK,    Gate.ALWAYS, 0xD8D8D8, 0xC9C9C9,  5,  1),
    GOLD      ("gold",      "Gold",      "金丝木",   "Waubeech",     Shape.OAK,    Gate.ALWAYS, 0xF6D64B, 0xF0D060,  6,  1),
    DIAMOND   ("diamond",   "Diamond",   "晶化木",   "Crystallized", Shape.SPRUCE, Gate.ALWAYS, 0x5CE0DC, 0x62D9D0,  9,  1),
    EMERALD   ("emerald",   "Emerald",   "翠晶树",   "Canutillos",   Shape.SPRUCE, Gate.ALWAYS, 0x3FD07A, 0x49CC7C,  8,  1),
    REDSTONE  ("redstone",  "Redstone",  "电木",     "Conductor",    Shape.SPRUCE, Gate.ALWAYS, 0xC0271A, 0xD5342A,  6, 16),
    LAPIS     ("lapis",     "Lapis",     "蓝琉璃木", "Lazuli",       Shape.SPRUCE, Gate.ALWAYS, 0x2C4FBF, 0x3560C8,  7, 12),
    COPPER    ("copper",    "Copper",    "紫金树",   "Violet",       Shape.OAK,    Gate.DETECT, 0xD07A3F, 0xD98A4A,  3,  1),
    SILVER    ("silver",    "Silver",    "白银木",   "Silver",       Shape.OAK,    Gate.DETECT, 0xC7D3DB, 0xCFD9E0,  4,  1),
    LEAD      ("lead",      "Lead",      "重木",     "Dense",        Shape.OAK,    Gate.DETECT, 0x6A6F8C, 0x767C99,  3,  1),
    TIN       ("tin",       "Tin",       "锡木",     "Tin",          Shape.OAK,    Gate.DETECT, 0xB9C4C8, 0xC2CCD0,  3,  1),
    RUBY      ("ruby",      "Ruby",      "红晶木",   "Ruby",         Shape.SPRUCE, Gate.DETECT, 0xC0274B, 0xCE3A5C,  6,  1),
    SAPPHIRE  ("sapphire",  "Sapphire",  "蓝石木",   "Sapphire",     Shape.SPRUCE, Gate.DETECT, 0x2E5FD0, 0x3A6FE0,  6,  1),
    PLATINUM  ("platinum",  "Platinum",  "白金木",   "Platinum",     Shape.SPRUCE, Gate.DETECT, 0xD1F6FF, 0xD1F6FF,  8,  1),
    NICKEL    ("nickel",    "Nickel",    "镍石木",   "Nickel",       Shape.SPRUCE, Gate.DETECT, 0xC5C39A, 0xCBC9A6,  7,  1),
    IRIDIUM   ("iridium",   "Iridium",   "光华木",   "Luminant",     Shape.SPRUCE, Gate.DETECT, 0xD6DCB8, 0xDCE2C2,  9,  1),
    TITANIUM  ("titanium",  "Titanium",  "金红树",   "Rutile",       Shape.SPRUCE, Gate.DETECT, 0xB6776B, 0xC0837A,  9,  1),
    URANIUM   ("uranium",   "Uranium",   "辐照木",   "Radiative",    Shape.OAK,    Gate.DETECT, 0x57C24A, 0x66D24F,  8,  1),
    NEON      ("neon",      "Neon",      "霓虹树",   "Neon",         Shape.SPRUCE, Gate.ALWAYS, 0xFF7CE0, 0xFF8CE6,  0,  0);

    /** 树形：决定生成算法 + 树叶掉落物种类 */
    public enum Shape {
        /** 橡树形 → 阔叶 broadleaf，基底用橡木材质 */
        OAK("broadleaf", "broadleaf", "oak"),
        /** 云杉形 → 针叶 needleleaf，基底用云杉材质 */
        SPRUCE("needleleaf", "needleleaf", "spruce");

        private final String dropSuffix;
        private final String dropOdPrefix;
        private final String textureBase;

        Shape(String dropSuffix, String dropOdPrefix, String textureBase) {
            this.dropSuffix = dropSuffix;
            this.dropOdPrefix = dropOdPrefix;
            this.textureBase = textureBase;
        }

        /** 掉落物 id 后缀：broadleaf / needleleaf */
        public String getDropSuffix() {
            return dropSuffix;
        }

        /** 掉落物矿词前缀：broadleaf / needleleaf */
        public String getDropOdPrefix() {
            return dropOdPrefix;
        }

        /** 基底材质：oak / spruce */
        public String getTextureBase() {
            return textureBase;
        }
    }

    /** 门控方式 */
    public enum Gate {
        /** 原本带有：永远启用 */
        ALWAYS,
        /** 检测到对应矿物 OD 才启用 */
        DETECT
    }

    private final String id;
    private final String odName;
    private final String chineseName;
    private final String englishName;
    private final Shape shape;
    private final Gate gate;
    private final int defaultLogColor;
    private final int defaultFoliageColor;
    /** 几个落叶物品能转化出矿物（0 = 不提供配方），来自 task.xls */
    private final int oreFromLeaves;
    /** 一次转化产出几个矿物（0 = 不提供配方），来自 task.xls */
    private final int oreYield;

    OreTreeType(String id, String odName, String chineseName, String englishName,
                Shape shape, Gate gate, int defaultLogColor, int defaultFoliageColor,
                int oreFromLeaves, int oreYield) {
        this.id = id;
        this.odName = odName;
        this.chineseName = chineseName;
        this.englishName = englishName;
        this.shape = shape;
        this.gate = gate;
        this.defaultLogColor = defaultLogColor;
        this.defaultFoliageColor = defaultFoliageColor;
        this.oreFromLeaves = oreFromLeaves;
        this.oreYield = oreYield;
    }

    // ------------------------------------------------------------------
    // 基本属性
    // ------------------------------------------------------------------

    /** id 前缀，如 {@code iron} */
    public String getId() {
        return id;
    }

    /** 矿词后缀，如 {@code Iron} */
    public String getOdName() {
        return odName;
    }

    /** 中文树名，如“铁树” */
    public String getChineseName() {
        return chineseName;
    }

    /** 英文树名，如 {@code Cycas} */
    public String getEnglishName() {
        return englishName;
    }

    public Shape getShape() {
        return shape;
    }

    public Gate getGate() {
        return gate;
    }

    // ------------------------------------------------------------------
    // 颜色（构建期用）
    //
    // defaultLogColor / defaultFoliageColor 是"默认参考色"；真正吃这两个颜色的是构建期的贴图生成器：
    //     tools/neotreeores-textures.json    颜色清单
    //     tools/TextureTinter.java           灰度基底 × 颜色 → 彩色材质（木材 multiply / 树叶 normalize）
    // 运行期不做 tint，所以改这两个值对游戏没影响。
    // ------------------------------------------------------------------
    public int getDefaultLogColor() {
        return defaultLogColor;
    }

    public int getDefaultFoliageColor() {
        return defaultFoliageColor;
    }

    /** 落叶转化消耗量（task.xls 第 7 列；0 = 不提供配方） */
    @Override
    public int getOreFromLeavesCount() {
        return oreFromLeaves;
    }

    /** 落叶转化产出量（task.xls 第 8 列；0 = 不提供配方） */
    @Override
    public int getOreYield() {
        return oreYield;
    }

    /** 用于查找：按 id 前缀 */
    public static OreTreeType byId(String id) {
        for (OreTreeType t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return null;
    }
}
