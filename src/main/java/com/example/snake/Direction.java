package com.example.snake;

/**
 * 四个移动方向。
 *
 * <p>每个方向自带一个位移量 (dx, dy)：dx 是列的变化，dy 是行的变化。
 * 屏幕坐标系里 y 轴朝下，所以"上"是 dy = -1，"下"是 dy = +1。
 *
 * <p>把这个约定写进枚举一次，后面所有跟方向有关的计算都不再需要 if/switch：
 * 走一步就是 (col + dx, row + dy)，判断反向就是一个加法。
 */
public enum Direction {

    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /** 列的变化量 */
    public int dx() {
        return dx;
    }

    /** 行的变化量 */
    public int dy() {
        return dy;
    }

    /**
     * 判断两个方向是否正好相反。
     *
     * <p>相反的两个方向，位移量相加必然是两个零：上(0,-1) 加 下(0,1) 得 (0,0)，
     * 左(-1,0) 加 右(1,0) 也得 (0,0)。而"上和左"相加得到 (-1,-1)、"上跟上"得到 (0,-2)，
     * 都不满足，所以这一个表达式就足以覆盖全部四种情况。
     */
    public boolean isOpposite(Direction other) {
        return other != null && dx + other.dx == 0 && dy + other.dy == 0;
    }
}
