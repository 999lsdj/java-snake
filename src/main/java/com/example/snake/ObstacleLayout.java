package com.example.snake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * 决定障碍物摆在哪些格子。
 *
 * <p>和 {@link FoodSpawner} 一样，把"随机摆放"抽成接口，测试才能用固定布局。
 *
 * <p>这里有一个必须遵守的约束：**障碍物不能挡住蛇的出生区**。
 * 否则开局第一帧蛇就撞死，玩家会以为程序坏了。生成器通过 {@code avoid} 参数
 * 拿到"绝对不能摆"的格子集合，由 {@link SnakeGame} 计算好传进来。
 */
@FunctionalInterface
public interface ObstacleLayout {

    /**
     * 生成障碍物。
     *
     * @param cols   场地列数
     * @param rows   场地行数
     * @param avoid  绝对不能摆放的格子（蛇的出生区）
     * @return 障碍物坐标集合
     */
    Set<GridPoint> generate(int cols, int rows, Set<GridPoint> avoid);

    /** 没有障碍物：给不关心障碍物的测试和简化场景用 */
    static ObstacleLayout none() {
        return (cols, rows, avoid) -> Set.of();
    }

    /**
     * 随机摆放指定数量的障碍物。
     *
     * <p>用洗牌后取前 N 个，等价于"不重复地随机抽 N 个"，比反复抽签去重更简单可靠。
     */
    static ObstacleLayout random(int count, Random random) {
        Objects.requireNonNull(random, "random 不能为 null");
        if (count < 0) {
            throw new IllegalArgumentException("障碍物数量不能为负数");
        }
        return (cols, rows, avoid) -> {
            List<GridPoint> candidates = new ArrayList<>();
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    GridPoint point = new GridPoint(col, row);
                    if (!avoid.contains(point)) {
                        candidates.add(point);
                    }
                }
            }
            Collections.shuffle(candidates, random);
            int take = Math.min(count, candidates.size());
            return Set.copyOf(candidates.subList(0, take));
        };
    }
}
