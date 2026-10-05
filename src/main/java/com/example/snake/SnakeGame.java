package com.example.snake;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * 游戏状态：蛇、食物、分数，以及"什么时候该走一格"。
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
        /** 撞到墙，已经停住 */
        HIT_WALL,
        /** 咬到自己，已经停住 */
        HIT_SELF,
        /** 整块场地被填满（实际很难达成，但必须处理，否则会出现空指针） */
        WON
    }

    /** 最多缓存两次待生效的转向，防止玩家快速连按导致中间那次被吞掉 */
    private static final int MAX_PENDING_TURNS = 2;

    private final int cols;
    private final int rows;
    private final int movesPerSecond;
    private final int ticksPerSecond;
    private final FoodSpawner foodSpawner;

    private final Snake snake;
    private final Deque<Direction> pendingTurns = new ArrayDeque<>();

    private Direction direction = Direction.RIGHT;
    private State state = State.RUNNING;
    private GridPoint food;
    private int score;
    private int moveAccumulator;
    private long moveCount;

    /** 正式运行用的构造器：初始长度 3，食物随机放置 */
    public SnakeGame(int cols, int rows, int movesPerSecond, int ticksPerSecond) {
        this(cols, rows, movesPerSecond, ticksPerSecond, 3, randomSpawner(new Random()));
    }

    /**
     * 完整构造器，供单元测试指定初始长度与食物策略。
     *
     * @param initialSnakeLength 初始长度，必须能放进场地（从中心往左放得下）
     * @param foodSpawner        食物生成策略
     */
    public SnakeGame(int cols, int rows, int movesPerSecond, int ticksPerSecond,
                     int initialSnakeLength, FoodSpawner foodSpawner) {
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
        this.movesPerSecond = movesPerSecond;
        this.ticksPerSecond = ticksPerSecond;
        this.foodSpawner = Objects.requireNonNull(foodSpawner, "foodSpawner 不能为 null");
        this.snake = new Snake(new GridPoint(initialHeadCol, rows / 2),
                initialSnakeLength, Direction.RIGHT);
        this.food = spawnFood();
    }

    /** 在所有空格子里随机挑一个 */
    private static FoodSpawner randomSpawner(Random random) {
        return (freeCells, head) -> freeCells.get(random.nextInt(freeCells.size()));
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

    /**
     * 走一格。判定顺序很重要：先看墙，再看自己，最后才处理吃食物。
     *
     * <p>自撞里有个经典细节：**不吃食物的时候，尾巴会在这一步让开**，
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
        }
        snake.move(direction);
        moveCount++;
        if (willEat) {
            food = spawnFood();
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
     * 重新放置食物：把所有没被蛇占据的格子列出来，交给策略挑一个。
     *
     * <p>这里用的是"枚举全部空格再挑"，而不是"随机取一个格子、撞上蛇就重抽"。
     * 后者在蛇很长的时候可能连续抽中很多次才成功，极端情况下甚至抽不出来
     * （虽然概率极低）。枚举法最多扫描一遍场地，行为稳定可预测。
     */
    private GridPoint spawnFood() {
        List<GridPoint> freeCells = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                GridPoint point = new GridPoint(col, row);
                if (!snake.occupies(point)) {
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

    /** 当前食物的位置；场地被填满时可能为 null */
    public GridPoint food() {
        return food;
    }

    /** 已经吃到几个食物 */
    public int score() {
        return score;
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
