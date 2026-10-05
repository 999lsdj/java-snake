package com.example.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 游戏状态的测试。
 *
 * <p>注意 {@code oneMovePerTick}：把"每秒移动次数"和"每秒帧数"都设成 60，
 * 于是每次 {@code tick()} 刚好走一格，断言里数帧就是在数格子，读起来直观。
 */
class SnakeGameTest {

    private static SnakeGame oneMovePerTick(int cols, int rows) {
        return new SnakeGame(cols, rows, 60, 60);
    }

    /**
     * 测试用的食物策略：把食物放在蛇头正右方（空着的话），否则退回第一个空格。
     *
     * <p>用它是为了让"吃到食物"这件事可预测——正式运行用的是随机位置，
     * 那样测试就没法断言吃之后会发生什么。
     */
    private static FoodSpawner foodToTheRight() {
        return (freeCells, head) -> {
            GridPoint right = head.move(Direction.RIGHT);
            return freeCells.contains(right) ? right : freeCells.get(0);
        };
    }

    @Test
    @DisplayName("速度换算：每秒 8 格时，跑满 60 帧正好走 8 格")
    void movesEightTimesPerSecond() {
        SnakeGame game = new SnakeGame(40, 28, 8, 60);

        for (int i = 0; i < 7; i++) {
            game.tick();
        }
        assertEquals(0, game.moveCount(), "前 7 帧还没攒够一次移动");

        for (int i = 0; i < 53; i++) {
            game.tick();
        }
        assertEquals(8, game.moveCount(), "跑满 60 帧应该正好走 8 格（平均 7.5 帧一格）");
    }

    @Test
    @DisplayName("撞墙：停在最后一格，状态变成 HIT_WALL")
    void stopsAtWall() {
        SnakeGame game = oneMovePerTick(5, 5);
        assertEquals(new GridPoint(2, 2), game.head());

        game.tick();    // 走到 (3,2)
        game.tick();    // 走到 (4,2)，最后一列
        game.tick();    // 再往右就越界

        assertEquals(SnakeGame.State.HIT_WALL, game.state());
        assertEquals(new GridPoint(4, 2), game.head(), "撞墙之后蛇头不应该越界");
        assertEquals(2, game.moveCount());
    }

    @Test
    @DisplayName("禁止 180 度反向：向右走时按左键无效")
    void rejectsReverse() {
        SnakeGame game = oneMovePerTick(10, 10);

        game.requestDirection(Direction.LEFT);
        game.tick();

        assertEquals(Direction.RIGHT, game.direction());
        assertEquals(new GridPoint(6, 5), game.head(), "仍然向右走了一格");
    }

    @Test
    @DisplayName("快速连按两次转向会被缓存，中间那次不会丢")
    void buffersTwoTurns() {
        SnakeGame game = oneMovePerTick(10, 10);

        game.requestDirection(Direction.UP);
        game.requestDirection(Direction.LEFT);
        assertEquals(2, game.pendingTurnCount());

        game.tick();    // 第一次转向生效：向上
        assertEquals(Direction.UP, game.direction());
        assertEquals(new GridPoint(5, 4), game.head());

        game.tick();    // 第二次转向生效：向左
        assertEquals(Direction.LEFT, game.direction());
        assertEquals(new GridPoint(4, 4), game.head());
    }

    @Test
    @DisplayName("撞墙之后不再移动，也不再接受转向")
    void ignoresInputAfterWall() {
        SnakeGame game = oneMovePerTick(5, 5);
        for (int i = 0; i < 3; i++) {
            game.tick();
        }
        long movesWhenStopped = game.moveCount();

        game.requestDirection(Direction.UP);
        game.tick();

        assertEquals(SnakeGame.State.HIT_WALL, game.state());
        assertEquals(movesWhenStopped, game.moveCount());
        assertEquals(0, game.pendingTurnCount(), "停止后按键被忽略");
    }

    @Test
    @DisplayName("构造函数挡住不合法的区域和速度参数")
    void rejectsIllegalArguments() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SnakeGame(2, 10, 8, 60));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SnakeGame(10, 10, 0, 60));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SnakeGame(10, 10, 61, 60));
        // 初始长度放不下：头在 col=5，往左只能再放 5 格，所以最长 6
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SnakeGame(10, 10, 8, 60, 7, foodToTheRight()));
    }

    @Test
    @DisplayName("吃到食物：长度加一、分数加一，新食物不会落在蛇身上")
    void eatsFoodAndGrows() {
        SnakeGame game = new SnakeGame(20, 20, 60, 60, 3, foodToTheRight());
        assertEquals(new GridPoint(11, 10), game.food(), "食物应该生成在蛇头右侧");
        assertEquals(0, game.score());

        game.tick();

        assertEquals(1, game.score());
        assertEquals(4, game.snakeLength());
        assertEquals(new GridPoint(11, 10), game.head());
        assertFalse(game.isOccupied(game.food()), "新食物必须放在空格子上");
    }

    @Test
    @DisplayName("连续吃五口：分数、长度同步增长，食物始终不在蛇身上")
    void keepsEatingAndGrowing() {
        SnakeGame game = new SnakeGame(20, 20, 60, 60, 3, foodToTheRight());

        for (int eaten = 1; eaten <= 5; eaten++) {
            game.tick();
            assertEquals(eaten, game.score());
            assertEquals(3 + eaten, game.snakeLength());
            assertFalse(game.isOccupied(game.food()));
        }
        assertEquals(SnakeGame.State.RUNNING, game.state());
    }

    @Test
    @DisplayName("咬到自己：头撞进身体就判负（长度 8 的蛇拐 U 形）")
    void stopsWhenBitingItself() {
        SnakeGame game = new SnakeGame(20, 20, 60, 60, 8, foodToTheRight());

        game.requestDirection(Direction.UP);
        game.tick();
        game.requestDirection(Direction.LEFT);
        game.tick();
        game.tick();
        game.requestDirection(Direction.DOWN);
        game.tick();     // 这一步头会进入自己的身体

        assertEquals(SnakeGame.State.HIT_SELF, game.state());
        assertEquals(new GridPoint(8, 9), game.head(), "撞到自己时蛇头停在原地");
        assertEquals(3, game.moveCount());
    }

    @Test
    @DisplayName("贴着尾巴走是合法的：不吃食物时尾巴会让开这一格")
    void mayEnterTheTailCell() {
        SnakeGame game = new SnakeGame(20, 20, 60, 60, 6, foodToTheRight());

        game.requestDirection(Direction.UP);
        game.tick();
        game.requestDirection(Direction.LEFT);
        game.tick();
        game.tick();
        game.requestDirection(Direction.DOWN);
        game.tick();     // 这一步头进入的正是尾巴当前所在的格子

        assertEquals(SnakeGame.State.RUNNING, game.state(), "尾巴会让开，所以不算撞");
        assertEquals(new GridPoint(8, 10), game.head());
        assertEquals(4, game.moveCount());
    }
}
