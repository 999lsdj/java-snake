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
    public static final String TITLE = "贪吃蛇 — java-snake M2";

    /** 画布宽度（像素） */
    public static final int WINDOW_WIDTH = 800;

    /** 画布高度（像素） */
    public static final int WINDOW_HEIGHT = 600;

    /** 顶部状态栏高度（像素）。这条区域不画网格，用来显示帧率、长度和方向 */
    public static final int HUD_HEIGHT = 40;

    /** 目标帧率：游戏循环每秒更新 60 次 */
    public static final int TARGET_FPS = 60;

    /** 网格尺寸：一格 20 像素 */
    public static final int TILE_SIZE = 20;

    /** 游戏区域有多少列（横向格子数） */
    public static final int COLS = WINDOW_WIDTH / TILE_SIZE;

    /** 游戏区域有多少行（纵向格子数），要扣掉顶部状态栏 */
    public static final int ROWS = (WINDOW_HEIGHT - HUD_HEIGHT) / TILE_SIZE;

    /**
     * 蛇每秒移动多少格。
     *
     * <p>注意它和帧率是两回事：循环每秒跑 60 帧，但蛇每秒只走 8 格。
     * 混为一谈的后果是蛇以每帧一格的速度飞出去，根本没法玩。
     */
    public static final int MOVES_PER_SECOND = 8;

    static {
        if (WINDOW_WIDTH % TILE_SIZE != 0) {
            throw new IllegalStateException("窗口宽度必须是网格尺寸的整数倍，否则最右边会缺一角");
        }
        if ((WINDOW_HEIGHT - HUD_HEIGHT) % TILE_SIZE != 0) {
            throw new IllegalStateException("扣掉状态栏之后的可用高度必须是网格尺寸的整数倍");
        }
        if (MOVES_PER_SECOND <= 0 || MOVES_PER_SECOND > TARGET_FPS) {
            throw new IllegalStateException("每秒移动次数必须落在 1 到 TARGET_FPS 之间");
        }
    }

    private GameConfig() {
        throw new AssertionError("GameConfig 只放常量，不应该被实例化");
    }
}
