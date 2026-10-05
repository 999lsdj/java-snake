package com.example.snake;

import java.util.List;

/**
 * 决定新食物放在哪个格子。
 *
 * <p>把"食物放哪"抽成一个接口，是为了两件事：
 * <ol>
 *   <li>正式运行时用随机策略，单元测试里换成确定策略——否则测试没法断言"吃到食物之后会怎样"，
 *       因为食物每次落在不同地方；</li>
 *   <li>将来想加"食物出现在离蛇更近的地方"这类玩法，只要换一个实现，不用动游戏逻辑。</li>
 * </ol>
 *
 * <p>这是一个函数式接口：只有一个抽象方法，所以可以用 lambda 直接写实现。
 */
@FunctionalInterface
public interface FoodSpawner {

    /**
     * 从空格子里挑一个放食物。
     *
     * @param freeCells 当前所有没被蛇占据的格子（保证非空）
     * @param head      蛇头当前位置，某些策略会想参考它
     * @return 选中的格子，必须来自 {@code freeCells}
     */
    GridPoint next(List<GridPoint> freeCells, GridPoint head);
}
