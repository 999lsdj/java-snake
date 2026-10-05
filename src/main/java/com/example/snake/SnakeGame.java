package com.example.snake;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * 游戏状态：蛇、当前方向，以及"什么时候该走一格"。
 *
 * <p>它完全不认识 Swing，所以能在没有图形界面的环境里跑单元测试——这是 M1 定下的规矩。
 *
 * <p><b>速度控制</b>复用了 M1 的思路：游戏循环每秒固定调用 {@link #tick()} 60 次，
 * 这里再用一个"蓄水"计数器把 60 次调用折算成每秒 8 次移动。M1 用水位攒的是纳秒，
 * 这里攒的是帧数，本质是同一个算法。
 */
public final class SnakeGame {

    /** 游戏状态 */
    public enum State {
        /** 正常进行中 */
        RUNNING,
        /** 撞到墙，已经停住。M4 会把它扩展成完整的结束与重开流程 */
        HIT_WALL
    }

    /** 最多缓存两次待生效的转向，防止玩家快速连按导致中间那次被吞掉 */
    private static final int MAX_PENDING_TURNS = 2;

    private final int cols;
    private final int rows;
    private final int movesPerSecond;
    private final int ticksPerSecond;

    private final Snake snake;
    private final Deque<Direction> pendingTurns = new ArrayDeque<>();

    private Direction direction = Direction.RIGHT;
    private State state = State.RUNNING;
    private int moveAccumulator;
    private long moveCount;

    public SnakeGame(int cols, int rows, int movesPerSecond, int ticksPerSecond) {
        if (cols < 3 || rows < 3) {
            throw new IllegalArgumentException("游戏区域至少 3 x 3");
        }
        if (movesPerSecond <= 0 || ticksPerSecond <= 0 || movesPerSecond > ticksPerSecond) {
            throw new IllegalArgumentException(
                    "速度参数不合法：movesPerSecond=" + movesPerSecond + " ticksPerSecond=" + ticksPerSecond);
        }
        this.cols = cols;
        this.rows = rows;
        this.movesPerSecond = movesPerSecond;
        this.ticksPerSecond = ticksPerSecond;

        // 初长度取 3，但不能超过从中心到左边缘的格子数，否则会一开始就跑到区域外面
        int initialLength = Math.min(3, cols / 2 + 1);
        this.snake = new Snake(new GridPoint(cols / 2, rows / 2), initialLength, Direction.RIGHT);
    }

    /**
     * 游戏循环每帧调用一次。
     *
     * <p>蓄水计数器的做法：每一帧往桶里加"每秒移动次数"，桶里的水够"每秒钟帧数"
     * 就舀出一勺、让蛇走一格。这样不管帧率和速度是否整除，长期平均速度都精确。
     * 比如 8 格/秒、60 帧/秒时，平均每 7.5 帧走一格。
     */
    public void tick() {
        if (state != State.RUNNING) {
            return;
        }
        moveAccumulator += movesPerSecond;
        if (moveAccumulator >= ticksPerSecond) {
            moveAccumulator -= ticksPerSecond;
            advance();
        }
    }

    /**
     * 玩家按方向键时调用。它只把请求排进队列，不立刻生效——真正的转向发生在
     * 下一次移动前，这样"按下按键的时刻"和"蛇转向的时刻"就解耦了。
     */
    public void requestDirection(Direction requested) {
        if (state != State.RUNNING || requested == null) {
            return;
        }
        // 以"最后一次已计划的转向"为基准来判断，而不是以当前方向为基准，
        // 否则连按两次时第二次会被误判成合法而最终导致自撞
        Direction planned = pendingTurns.isEmpty() ? direction : pendingTurns.peekLast();
        if (requested == planned || requested.isOpposite(planned)) {
            return;
        }
        if (pendingTurns.size() >= MAX_PENDING_TURNS) {
            return;
        }
        pendingTurns.addLast(requested);
    }

    /** 走一格：先应用待生效的转向，再判断新蛇头会不会越界 */
    private void advance() {
        applyPendingTurn();
        GridPoint nextHead = snake.head().move(direction);
        if (!isInside(nextHead)) {
            state = State.HIT_WALL;
            return;
        }
        snake.move(direction);
        moveCount++;
    }

    /**
     * 把队列里最早的一次转向落到 {@code direction} 上。
     *
     * <p>这里要再过滤一次反向：排队的两次转向之间也可能构成反向
     * （比如向右走时先按上、再按下，两次请求都合法，但连起来是原地折返）。
     */
    private void applyPendingTurn() {
        while (!pendingTurns.isEmpty()) {
            Direction queued = pendingTurns.pollFirst();
            if (queued != direction && !queued.isOpposite(direction)) {
                direction = queued;
                return;
            }
        }
    }

    private boolean isInside(GridPoint point) {
        return point.col() >= 0 && point.col() < cols
                && point.row() >= 0 && point.row() < rows;
    }

    public GridPoint head() {
        return snake.head();
    }

    public List<GridPoint> body() {
        return snake.body();
    }

    public int snakeLength() {
        return snake.length();
    }

    /** 蛇是否占据某一格（M3 放食物时要靠它避开蛇身） */
    public boolean isOccupied(GridPoint point) {
        return snake.occupies(point);
    }

    public Direction direction() {
        return direction;
    }

    public State state() {
        return state;
    }

    public int cols() {
        return cols;
    }

    public int rows() {
        return rows;
    }

    /** 已经走了多少格，用于界面显示和测试断言 */
    public long moveCount() {
        return moveCount;
    }

    /** 还排着几次没生效的转向，仅用于测试与调试显示 */
    public int pendingTurnCount() {
        return pendingTurns.size();
    }
}
