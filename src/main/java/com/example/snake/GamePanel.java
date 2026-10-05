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

import javax.swing.JPanel;

/**
 * 画布：把游戏状态画出来，并把键盘输入转成方向请求。
 *
 * <p>Swing 的铁律：所有对界面组件的操作都在事件分发线程（EDT）上做，
 * 唯一的例外是 {@code repaint()}——它被设计成线程安全，所以游戏循环可以直接调用。
 * 本类里除了 {@code paintComponent} 之外的字段都不参与跨线程读写，原因是：
 * 状态本身放在 {@link SnakeGame} 里，而它只在游戏线程上被改动。
 *
 * <p>绘制分四层，从下到上：状态栏 → 网格 → 蛇 → 结束提示。
 */
public final class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color BACKGROUND = new Color(0x11141A);
    private static final Color GRID = new Color(0x1E2630);
    private static final Color HUD_BACKGROUND = new Color(0x161C24);
    private static final Color SNAKE_HEAD = new Color(0x6EE7B7);
    private static final Color SNAKE_BODY = new Color(0x2F9E6B);
    private static final Color FOOD = new Color(0xF0883E);
    private static final Color TEXT = new Color(0xE6EDF3);
    private static final Color MUTED = new Color(0x8B949E);
    private static final Color WARNING = new Color(0xF97583);

    private final SnakeGame game;

    /** FPS 统计只在 EDT 上读写，不需要 volatile */
    private long fpsWindowStartNanos = System.nanoTime();
    private int framesInWindow;
    private int fps;

    public GamePanel(SnakeGame game) {
        this.game = game;
        setPreferredSize(new Dimension(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT));
        setBackground(BACKGROUND);
        setFocusable(true);
        // 方向键默认被 Swing 拿去在组件之间移动焦点，不关掉的话 keyPressed 根本收不到
        setFocusTraversalKeysEnabled(false);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
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
        game.tick();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawGrid(g2);
        drawFood(g2);
        drawSnake(g2);
        drawHud(g2);
        if (game.state() != SnakeGame.State.RUNNING) {
            drawGameOverHint(g2);
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

    private void drawHud(Graphics2D g2) {
        g2.setColor(HUD_BACKGROUND);
        g2.fillRect(0, 0, getWidth(), GameConfig.HUD_HEIGHT);

        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        g2.setColor(TEXT);
        g2.drawString("分数 " + game.score()
                + "    长度 " + game.snakeLength()
                + "    方向 " + arrow(game.direction())
                + "    实测 " + fps + " fps", 12, 25);

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        g2.setColor(MUTED);
        String hint = "方向键控制方向";
        int width = g2.getFontMetrics().stringWidth(hint);
        g2.drawString(hint, getWidth() - width - 12, 26);
    }

    private void drawGameOverHint(Graphics2D g2) {
        int top = GameConfig.HUD_HEIGHT;
        g2.setColor(new Color(0x11, 0x14, 0x1A, 0xCC));
        g2.fillRect(0, top, getWidth(), getHeight() - top);

        String title = switch (game.state()) {
            case HIT_WALL -> "撞到墙了！";
            case HIT_SELF -> "咬到自己了！";
            case WON -> "整块场地都被你填满了！";
            case RUNNING -> "";
        };
        String sub = "本局得分 " + game.score() + "（M4 会加 R 键快速重开）";

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        int titleWidth = g2.getFontMetrics().stringWidth(title);
        g2.setColor(WARNING);
        g2.drawString(title, (getWidth() - titleWidth) / 2, getHeight() / 2 - 10);

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        int subWidth = g2.getFontMetrics().stringWidth(sub);
        g2.setColor(MUTED);
        g2.drawString(sub, (getWidth() - subWidth) / 2, getHeight() / 2 + 30);
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
