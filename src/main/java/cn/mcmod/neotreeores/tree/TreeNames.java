package cn.mcmod.neotreeores.tree;

import net.minecraft.util.text.translation.I18n;

/**
 * 方块/物品显示名。
 *
 * <p>内置 19 棵树的名字写在 lang 文件里（中英都有）；脚本新增树是运行期才知道的，
 * 没法写文件，所以这里现场按 OD 名拼一个可读名字（{@code Quartz -> "Quartz Log"}）。</p>
 *
 * <p>优先级：<b>lang 里有就用 lang</b>（{@link I18n#canTranslate(String)}），否则用拼出来的名字。
 * 这样资源包只要提供同名 key 就能覆盖脚本树的名字，而且不依赖运行期注入语言表
 * （注入会被资源重载清掉，其它 mod 也可能提前缓存过名字）。</p>
 */
public final class TreeNames {

    private TreeNames() {
    }

    public static String log(IOreTree tree) {
        return pick(tree, tree.getLogLangKey(), " Log");
    }

    public static String wood(IOreTree tree) {
        return pick(tree, tree.getWoodLangKey(), " Wood");
    }

    public static String leaves(IOreTree tree) {
        return pick(tree, tree.getLeavesLangKey(), " Leaves");
    }

    public static String sapling(IOreTree tree) {
        return pick(tree, tree.getSaplingLangKey(), " Sapling");
    }

    public static String fallenLeaves(IOreTree tree) {
        String key = tree.getFallenLeavesLangKey();
        if (I18n.canTranslate(key)) {
            return I18n.translateToLocal(key);
        }
        return "Fallen " + base(tree) + " Leaves";
    }

    /** 树叶掉落物（阔叶/针叶） */
    public static String leafDrop(IOreTree tree) {
        String key = tree.getLeafDropLangKey();
        if (I18n.canTranslate(key)) {
            return I18n.translateToLocal(key);
        }
        String suffix = tree.getShape() == OreTreeType.Shape.OAK ? "Broadleaf" : "Needleleaf";
        return base(tree) + " " + suffix;
    }

    private static String pick(IOreTree tree, String langKey, String suffix) {
        if (I18n.canTranslate(langKey)) {
            return I18n.translateToLocal(langKey);
        }
        return base(tree) + suffix;
    }

    /** 兜底用的名字主体：英文名（内置树的英文树名 / 脚本树的 OD 名） */
    private static String base(IOreTree tree) {
        String name = tree.getEnglishName();
        if (name == null || name.trim().isEmpty()) {
            name = tree.getId();
        }
        return name;
    }
}
