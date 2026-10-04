package com.example.snake;

/**
 * 全局常量。
 *
 * <p>把"魔数"集中到一个地方，好处是改窗口大小时只需要改这里一处；
 * 坏处是常量之间可能互相矛盾（比如网格尺寸除不尽窗口宽度），所以下面加了断言。
 */
public final class GameConfig {

    /** 窗口标题 */
    public static final String TITLE = "贪吃蛇 — java-snake M1";

    /** 画布宽度（像素） */
    public static final int WINDOW_WIDTH = 800;

    /** 画布高度（像素） */
    public static final int WINDOW_HEIGHT = 600;

    /** 目标帧率：每秒更新 60 次 */
    public static final int TARGET_FPS = 60;

    /** 网格尺寸：一格 20 像素，M2 开始蛇和食物都会落在网格上 */
    public static final int TILE_SIZE = 20;

    static {
        if (WINDOW_WIDTH % TILE_SIZE != 0 || WINDOW_HEIGHT % TILE_SIZE != 0) {
            throw new IllegalStateException("窗口宽高必须是网格尺寸的整数倍，否则边界会算错");
        }
    }

    private GameConfig() {
        throw new AssertionError("GameConfig 只放常量，不应该被实例化");
    }
}
