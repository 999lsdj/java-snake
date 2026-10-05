package com.example.snake;

/**
 * 全局常量。
 *
 * <p>把窗口尺寸、帧率、网格、速度这些"魔数"集中在一处，改一个地方就能全局生效。
 * 类末尾的静态检查把"常量之间互相矛盾"的情况在启动时就暴露出来——比如窗口宽度除不尽
 * 网格尺寸，最右边会多出一条无法落脚的区域，而这种错在画面上很难一眼看出来。
 */
public final class GameConfig {

    /** 窗口标题 */
    public static final String TITLE = "贪吃蛇 — java-snake 1.0";

    /** 画布宽度（像素） */
    public static final int WINDOW_WIDTH = 800;

    /** 画布高度（像素） */
    public static final int WINDOW_HEIGHT = 600;

    /** 顶部状态栏高度（像素）。这条区域不画网格，用来显示分数、速度等信息 */
    public static final int HUD_HEIGHT = 40;

    /** 目标帧率：游戏循环每秒更新 60 次 */
    public static final int TARGET_FPS = 60;

    /** 网格尺寸：一格 20 像素 */
    public static final int TILE_SIZE = 20;

    /** 游戏区域有多少列（横向格子数） */
    public static final int COLS = WINDOW_WIDTH / TILE_SIZE;

    /** 游戏区域有多少行（纵向格子数），要扣掉顶部状态栏 */
    public static final int ROWS = (WINDOW_HEIGHT - HUD_HEIGHT) / TILE_SIZE;

    /** 开局速度：每秒移动多少格（之后每吃 5 个食物自动加一档） */
    public static final int INITIAL_MOVES_PER_SECOND = 8;

    /** 蛇的初始长度（格） */
    public static final int INITIAL_SNAKE_LENGTH = 3;

    /** 每局随机摆放多少个障碍物 */
    public static final int OBSTACLE_COUNT = 14;

    static {
        if (WINDOW_WIDTH % TILE_SIZE != 0) {
            throw new IllegalStateException("窗口宽度必须是网格尺寸的整数倍，否则最右边会缺一角");
        }
        if ((WINDOW_HEIGHT - HUD_HEIGHT) % TILE_SIZE != 0) {
            throw new IllegalStateException("扣掉状态栏之后的可用高度必须是网格尺寸的整数倍");
        }
        if (INITIAL_MOVES_PER_SECOND <= 0 || INITIAL_MOVES_PER_SECOND > TARGET_FPS) {
            throw new IllegalStateException("每秒移动次数必须落在 1 到 TARGET_FPS 之间");
        }
        if (INITIAL_SNAKE_LENGTH < 1) {
            throw new IllegalStateException("蛇的初始长度至少为 1");
        }
        if (OBSTACLE_COUNT < 0) {
            throw new IllegalStateException("障碍物数量不能为负数");
        }
    }

    private GameConfig() {
        throw new AssertionError("GameConfig 只放常量，不应该被实例化");
    }
}
