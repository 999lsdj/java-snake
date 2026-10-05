package com.example.snake;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;

/**
 * 画布：把游戏状态画出来，并把键盘输入转成"方向请求"和"重开请求"。
 *
 * <p><b>线程规则</b>：Swing 的所有界面操作都在事件分发线程（EDT）上做，
 * 而游戏状态只允许在游戏线程上改动。两者之间靠两个通道沟通：
 * <ul>
 *   <li>{@code repaint()} —— 唯一允许跨线程调用的界面方法，游戏线程直接调；</li>
 *   <li>一个 volatile 的"重开请求"标志 —— EDT 只负责立旗子，真正的重置动作
 *       由游戏线程在下一帧开头执行，这样状态永远只有一条线程在改。</li>
 * </ul>
 * 这和 M2 里"按键先入队、移动时才生效"是同一个思路。
 */
public final class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color BACKGROUND = new Color(0x11141A);
    private static final Color GRID = new Color(0x1E2630);
    private static final Color HUD_BACKGROUND = new Color(0x161C24);
    private static final Color SNAKE_HEAD = new Color(0x6EE7B7);
    private static final Color SNAKE_BODY = new Color(0x2F9E6B);
    private static final Color FOOD = new Color(0xF0883E);
    private static final Color OBSTACLE = new Color(0x4A5568);
    private static final Color TEXT = new Color(0xE6EDF3);
    private static final Color MUTED = new Color(0x8B949E);
    private static final Color WARNING = new Color(0xF97583);

    private final SnakeGame game;
    private final HighScoreStore highScoreStore;

    /** 最高分只在游戏线程上更新（updateGame），绘制时读取 */
    private volatile int bestScore;

    /** 上一次看到的游戏状态，用来判断"这一帧刚刚死掉" */
    private SnakeGame.State lastState = SnakeGame.State.RUNNING;

    /** EDT 立下的重开旗子，由游戏线程消费 */
    private volatile boolean restartRequested;

    /** FPS 统计只在 EDT 上读写 */
    private long fpsWindowStartNanos = System.nanoTime();
    private int framesInWindow;
    private int fps;

    public GamePanel(SnakeGame game, HighScoreStore highScoreStore) {
        this.game = game;
        this.highScoreStore = highScoreStore;
        this.bestScore = highScoreStore.load();

        setPreferredSize(new Dimension(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT));
        setBackground(BACKGROUND);
        setFocusable(true);
        // 方向键默认被 Swing 拿去在组件之间移动焦点，不关掉的话 keyPressed 根本收不到
        setFocusTraversalKeysEnabled(false);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_R) {
                    if (game.state() != SnakeGame.State.RUNNING) {
                        restartRequested = true;
                    }
                    return;
                }
                Direction requested = toDirection(e.getKeyCode());
                if (requested != null) {
                    game.requestDirection(requested);
                }
            }
        });
    }

    /** 把按键码翻译成方向；不是方向键就返回 null */
    private static Direction toDirection(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_UP -> Direction.UP;
            case KeyEvent.VK_DOWN -> Direction.DOWN;
            case KeyEvent.VK_LEFT -> Direction.LEFT;
            case KeyEvent.VK_RIGHT -> Direction.RIGHT;
            default -> null;
        };
    }

    /** 由 {@link GameLoop} 在游戏线程上按固定帧率调用 */
    public void updateGame() {
        if (restartRequested) {
            restartRequested = false;
            game.reset();
            lastState = SnakeGame.State.RUNNING;
        }

        game.tick();

        // 刚死掉的那一帧：刷新最高分并落盘
        SnakeGame.State now = game.state();
        if (lastState == SnakeGame.State.RUNNING && now != SnakeGame.State.RUNNING) {
            if (game.score() > bestScore) {
                bestScore = game.score();
                highScoreStore.save(bestScore);
            }
        }
        lastState = now;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawGrid(g2);
        drawObstacles(g2);
        drawFood(g2);
        drawSnake(g2);
        drawHud(g2);
        if (game.state() != SnakeGame.State.RUNNING) {
            drawGameOverScreen(g2);
        }
        countFrame();
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(GRID);
        int top = GameConfig.HUD_HEIGHT;
        for (int col = 0; col <= game.cols(); col++) {
            int x = col * GameConfig.TILE_SIZE;
            g2.drawLine(x, top, x, getHeight());
        }
        for (int row = 0; row <= game.rows(); row++) {
            int y = top + row * GameConfig.TILE_SIZE;
            g2.drawLine(0, y, getWidth(), y);
        }
    }

    /** 障碍物画成灰色实心方块，和蛇、食物一眼就能区分 */
    private void drawObstacles(Graphics2D g2) {
        g2.setColor(OBSTACLE);
        Set<GridPoint> obstacles = game.obstacles();
        for (GridPoint point : obstacles) {
            g2.fillRoundRect(point.col() * GameConfig.TILE_SIZE + 1,
                    GameConfig.HUD_HEIGHT + point.row() * GameConfig.TILE_SIZE + 1,
                    GameConfig.TILE_SIZE - 2,
                    GameConfig.TILE_SIZE - 2,
                    4, 4);
        }
    }

    /** 食物画成圆形，和方形的蛇一眼就能区分开 */
    private void drawFood(Graphics2D g2) {
        GridPoint food = game.food();
        if (food == null) {
            return;
        }
        g2.setColor(FOOD);
        int size = GameConfig.TILE_SIZE - 6;
        g2.fillOval(food.col() * GameConfig.TILE_SIZE + 3,
                GameConfig.HUD_HEIGHT + food.row() * GameConfig.TILE_SIZE + 3,
                size, size);
    }

    private void drawSnake(Graphics2D g2) {
        List<GridPoint> body = game.body();
        for (int i = 0; i < body.size(); i++) {
            GridPoint point = body.get(i);
            g2.setColor(i == 0 ? SNAKE_HEAD : SNAKE_BODY);
            // 每格留 2 像素空隙，让蛇身的分节看得出来
            g2.fillRect(point.col() * GameConfig.TILE_SIZE + 2,
                    GameConfig.HUD_HEIGHT + point.row() * GameConfig.TILE_SIZE + 2,
                    GameConfig.TILE_SIZE - 4,
                    GameConfig.TILE_SIZE - 4);
        }
    }

    private void drawHud(Graphics2D g2) {
        g2.setColor(HUD_BACKGROUND);
        g2.fillRect(0, 0, getWidth(), GameConfig.HUD_HEIGHT);

        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        g2.setColor(TEXT);
        g2.drawString("分数 " + game.score()
                + "   最高 " + bestScore
                + "   长度 " + game.snakeLength()
                + "   速度 " + game.movesPerSecond() + " 格/秒"
                + "   方向 " + arrow(game.direction())
                + "   " + fps + " fps", 12, 25);

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        g2.setColor(MUTED);
        String hint = game.state() == SnakeGame.State.RUNNING ? "方向键转向" : "按 R 重新开始";
        int width = g2.getFontMetrics().stringWidth(hint);
        g2.drawString(hint, getWidth() - width - 12, 26);
    }

    private void drawGameOverScreen(Graphics2D g2) {
        int top = GameConfig.HUD_HEIGHT;
        g2.setColor(new Color(0x11, 0x14, 0x1A, 0xCC));
        g2.fillRect(0, top, getWidth(), getHeight() - top);

        String title = switch (game.state()) {
            case HIT_WALL -> "撞到墙了！";
            case HIT_SELF -> "咬到自己了！";
            case HIT_OBSTACLE -> "撞到障碍物了！";
            case WON -> "整块场地都被你填满了！";
            case RUNNING -> "";
        };
        String sub = "本局得分 " + game.score() + "　最高分 " + bestScore + "　按 R 重新开始";

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        int titleWidth = g2.getFontMetrics().stringWidth(title);
        g2.setColor(WARNING);
        g2.drawString(title, (getWidth() - titleWidth) / 2, getHeight() / 2 - 10);

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        int subWidth = g2.getFontMetrics().stringWidth(sub);
        g2.setColor(TEXT);
        g2.drawString(sub, (getWidth() - subWidth) / 2, getHeight() / 2 + 34);
    }

    private static String arrow(Direction direction) {
        return switch (direction) {
            case UP -> "↑";
            case DOWN -> "↓";
            case LEFT -> "←";
            case RIGHT -> "→";
        };
    }

    private void countFrame() {
        framesInWindow++;
        long now = System.nanoTime();
        if (now - fpsWindowStartNanos >= 1_000_000_000L) {
            fps = framesInWindow;
            framesInWindow = 0;
            fpsWindowStartNanos = now;
        }
    }
}
