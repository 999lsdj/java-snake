package com.example.snake;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * 游戏状态：蛇、食物、障碍物、分数，以及"什么时候该走一格"。
 *
 * <p>它完全不认识 Swing，所以能在没有图形界面的环境里跑单元测试——这是 M1 定下的规矩。
 * 随机性和文件读写也都不在这里，而是通过 {@link FoodSpawner}、{@link ObstacleLayout}
 * 注入进来，于是这个类本身是完全确定的、可测的。
 *
 * <p><b>速度控制</b>复用了 M1 的思路：游戏循环每秒固定调用 {@link #tick()} 60 次，
 * 这里再用一个"蓄水"计数器把 60 次调用折算成每秒若干次移动。M1 用水位攒的是纳秒，
 * 这里攒的是帧数，本质是同一个算法。
 */
public final class SnakeGame {

    /** 游戏状态 */
    public enum State {
        /** 正常进行中 */
        RUNNING,
        /** 撞到墙 */
        HIT_WALL,
        /** 咬到自己 */
        HIT_SELF,
        /** 撞到障碍物 */
        HIT_OBSTACLE,
        /** 整块场地被填满（实际很难达成，但必须处理，否则会出现空指针） */
        WON
    }

    /** 最多缓存两次待生效的转向，防止玩家快速连按导致中间那次被吞掉 */
    private static final int MAX_PENDING_TURNS = 2;

    /** 每吃到这么多食物，速度提升一档 */
    private static final int SCORE_PER_SPEEDUP = 5;

    /** 速度上限：再快就不是给人玩的了 */
    private static final int MAX_MOVES_PER_SECOND = 15;

    private final int cols;
    private final int rows;
    private final int ticksPerSecond;
    private final int initialMovesPerSecond;
    private final int initialSnakeLength;
    private final FoodSpawner foodSpawner;
    private final ObstacleLayout obstacleLayout;

    private final Deque<Direction> pendingTurns = new ArrayDeque<>();

    private Snake snake;
    private Set<GridPoint> obstacles = Set.of();
    private Direction direction = Direction.RIGHT;
    private State state = State.RUNNING;
    private boolean paused;
    private GridPoint food;
    private int movesPerSecond;
    private int score;
    private int moveAccumulator;
    private long moveCount;

    /** 正式运行用的构造器：初始长度 3，食物与障碍物都随机 */
    public SnakeGame(int cols, int rows, int movesPerSecond, int ticksPerSecond) {
        this(cols, rows, movesPerSecond, ticksPerSecond, 3,
                FoodSpawner.random(new Random()), ObstacleLayout.none());
    }

    /** 指定初始长度与食物策略（测试常用），无障碍物 */
    public SnakeGame(int cols, int rows, int movesPerSecond, int ticksPerSecond,
                     int initialSnakeLength, FoodSpawner foodSpawner) {
        this(cols, rows, movesPerSecond, ticksPerSecond, initialSnakeLength,
                foodSpawner, ObstacleLayout.none());
    }

    /**
     * 完整构造器。
     *
     * @param initialSnakeLength 初始长度，必须从中心往左放得下
     * @param foodSpawner        食物生成策略
     * @param obstacleLayout     障碍物布局策略
     */
    public SnakeGame(int cols, int rows, int movesPerSecond, int ticksPerSecond,
                     int initialSnakeLength, FoodSpawner foodSpawner, ObstacleLayout obstacleLayout) {
        if (cols < 3 || rows < 3) {
            throw new IllegalArgumentException("游戏区域至少 3 x 3");
        }
        if (movesPerSecond <= 0 || ticksPerSecond <= 0 || movesPerSecond > ticksPerSecond) {
            throw new IllegalArgumentException(
                    "速度参数不合法：movesPerSecond=" + movesPerSecond + " ticksPerSecond=" + ticksPerSecond);
        }
        int initialHeadCol = cols / 2;
        if (initialSnakeLength < 1 || initialSnakeLength > initialHeadCol + 1) {
            throw new IllegalArgumentException(
                    "初始长度必须在 1 到 " + (initialHeadCol + 1) + " 之间，实际收到 " + initialSnakeLength);
        }

        this.cols = cols;
        this.rows = rows;
        this.ticksPerSecond = ticksPerSecond;
        this.initialMovesPerSecond = movesPerSecond;
        this.initialSnakeLength = initialSnakeLength;
        this.foodSpawner = Objects.requireNonNull(foodSpawner, "foodSpawner 不能为 null");
        this.obstacleLayout = Objects.requireNonNull(obstacleLayout, "obstacleLayout 不能为 null");

        startNewRound();
    }

    /** 开一局新的：重建蛇、重摆障碍物、重放食物、分数与速度清零 */
    public void reset() {
        startNewRound();
    }

    private void startNewRound() {
        this.direction = Direction.RIGHT;
        this.state = State.RUNNING;
        this.paused = false;
        this.score = 0;
        this.moveCount = 0;
        this.moveAccumulator = 0;
        this.movesPerSecond = initialMovesPerSecond;
        this.pendingTurns.clear();
        this.snake = new Snake(startHead(), initialSnakeLength, Direction.RIGHT);
        this.obstacles = obstacleLayout.generate(cols, rows, startAreaCells());
        this.food = spawnFood();
    }

    private GridPoint startHead() {
        return new GridPoint(cols / 2, rows / 2);
    }

    /**
     * 出生区：蛇开局占的位置，加上上方一格、下方一格，以及头前方三格。
     *
     * <p>障碍物生成器必须避开这一片，否则一开局蛇就撞死在障碍物上。
     */
    private Set<GridPoint> startAreaCells() {
        Set<GridPoint> cells = new HashSet<>();
        GridPoint head = startHead();
        for (int offset = -3; offset < initialSnakeLength; offset++) {
            int col = head.col() - offset;
            cells.add(new GridPoint(col, head.row()));
            cells.add(new GridPoint(col, head.row() - 1));
            cells.add(new GridPoint(col, head.row() + 1));
        }
        return cells;
    }

    /**
     * 游戏循环每帧调用一次。
     *
     * <p>蓄水计数器的做法：每一帧往桶里加"每秒移动次数"，桶里的水够"每秒钟帧数"
     * 就舀出一勺、让蛇走一格。这样不管帧率和速度是否整除，长期平均速度都精确。
     * 比如 8 格/秒、60 帧/秒时，平均每 7.5 帧走一格。
     */
    public void tick() {
        if (state != State.RUNNING || paused) {
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
        if (state != State.RUNNING || paused || requested == null) {
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

    /**
     * 暂停 / 继续，由空格键触发。
     *
     * <p><b>为什么暂停不是一个 {@link State}？</b>因为这个枚举表达的是"这一局怎么结束的"，
     * 除了 RUNNING 之外的四个值都是终局状态；而暂停是临时的、可以随时恢复的。混在一起会立刻带来两个坑：
     * 一是绘制层要专门判断"这个非 RUNNING 状态到底要不要显示结束画面"，
     * 二是"刚结束时保存最高分"的逻辑会把暂停误判成结束，暂停一下就存一次分。
     * 所以这里用独立的布尔量：语义清楚，也不会污染结束状态的判定。
     *
     * <p>已经结束的局不允许暂停。
     */
    public void togglePause() {
        if (state == State.RUNNING) {
            paused = !paused;
        }
    }

    /** 是否处于暂停状态 */
    public boolean isPaused() {
        return paused;
    }

    /**
     * 走一格。判定顺序很重要：**墙 → 障碍物 → 自己 → 吃食物**。
     *
     * <p>自撞里有个经典细节：不吃食物的时候，尾巴会在这一步让开，
     * 所以"蛇头进入尾巴当前所在的格子"是合法的，不算撞。漏掉这条，
     * 玩家贴着尾巴走就会莫名其妙地死。
     */
    private void advance() {
        applyPendingTurn();
        GridPoint nextHead = snake.head().move(direction);

        if (!isInside(nextHead)) {
            state = State.HIT_WALL;
            return;
        }
        if (obstacles.contains(nextHead)) {
            state = State.HIT_OBSTACLE;
            return;
        }

        boolean willEat = nextHead.equals(food);
        boolean followsTail = !willEat && nextHead.equals(snake.tail());
        if (!followsTail && snake.occupies(nextHead)) {
            state = State.HIT_SELF;
            return;
        }

        if (willEat) {
            // 必须在 move 之前标记生长，否则尾巴会先被砍掉，蛇就长不起来了
            snake.grow();
            score++;
            speedUpIfNeeded();
        }
        snake.move(direction);
        moveCount++;
        if (willEat) {
            food = spawnFood();
        }
    }

    /** 每吃满 5 个食物提速一档，直到上限 */
    private void speedUpIfNeeded() {
        if (score % SCORE_PER_SPEEDUP == 0 && movesPerSecond < MAX_MOVES_PER_SECOND) {
            movesPerSecond++;
        }
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

    /**
     * 重新放置食物：把所有没被蛇和障碍物占据的格子列出来，交给策略挑一个。
     *
     * <p>这里用的是"枚举全部空格再挑"，而不是"随机取一个格子、撞上就重抽"。
     * 后者在场地快被占满时可能连续抽不中，行为不可预测。枚举法最多扫描一遍场地。
     */
    private GridPoint spawnFood() {
        List<GridPoint> freeCells = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                GridPoint point = new GridPoint(col, row);
                if (!snake.occupies(point) && !obstacles.contains(point)) {
                    freeCells.add(point);
                }
            }
        }
        if (freeCells.isEmpty()) {
            state = State.WON;
            return null;
        }
        return foodSpawner.next(freeCells, snake.head());
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

    /** 蛇是否占据某一格 */
    public boolean isOccupied(GridPoint point) {
        return snake.occupies(point);
    }

    /** 障碍物坐标（不可变集合） */
    public Set<GridPoint> obstacles() {
        return obstacles;
    }

    /** 当前食物的位置；场地被填满时可能为 null */
    public GridPoint food() {
        return food;
    }

    /** 已经吃到几个食物 */
    public int score() {
        return score;
    }

    /** 当前每秒移动多少格（吃到食物后会提升） */
    public int movesPerSecond() {
        return movesPerSecond;
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
