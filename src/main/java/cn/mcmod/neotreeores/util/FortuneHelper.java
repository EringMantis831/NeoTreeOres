package cn.mcmod.neotreeores.util;

import java.util.Random;

/**
 * 时运加成，公式原样照抄原版矿物（{@code BlockOre.quantityDroppedWithBonus}）。
 *
 * <pre>
 * fortune == 0            → 1
 * fortune &gt; 0             → 1 + max(0, rand.nextInt(fortune + 2) - 1)
 * </pre>
 *
 * 也就是：1 级时运掉 1~2 个（平均 1.33），3 级时运掉 1~4 个（平均 2.5），
 * 与挖钻石/青金石等原版矿物被时运加成的"数量"分布完全一致。
 *
 * <p>注意：原版矿物受时运影响改的是<b>数量</b>，不是"掉落概率"，
 * 所以本 mod 的树叶掉落同样是「概率照配置不变，命中后按本公式加数量」。</p>
 */
public final class FortuneHelper {

    private FortuneHelper() {
    }

    /** 原版矿物在给定等级时运下的掉落数量 */
    public static int bonusCount(int fortune, Random rand) {
        if (fortune <= 0) {
            return 1;
        }
        int i = rand.nextInt(fortune + 2) - 1;
        if (i < 0) {
            i = 0;
        }
        return i + 1;
    }

    /** 同上，但把"额外部分"再乘以倍数（给落叶地毯的配置项用；倍数 1 = 原版行为） */
    public static int bonusCount(int fortune, Random rand, int scale) {
        if (scale <= 1) {
            return bonusCount(fortune, rand);
        }
        return 1 + (bonusCount(fortune, rand) - 1) * scale;
    }
}
