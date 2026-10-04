package com.example.snake;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;

/**
 * 画布：负责画背景和状态文字，并接收键盘输入。
 *
 * <p>Swing 有一条铁律：**所有对界面组件的读写都在事件分发线程（EDT）上做**。
 * 例外是 {@code repaint()}，它被设计成线程安全的，所以游戏循环可以直接调用它。
 * 反过来说，游戏循环里千万不要直接调 {@code setText}、{@code add} 这类方法。
 *
 * <p>M1 阶段这里只画一个占位的网格和状态文字，蛇和食物从 M2 开始出现。
 */
public final class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color BACKGROUND = new Color(0x11141A);
    private static final Color GRID = new Color(0x1E2630);
    private static final Color FOREGROUND = new Color(0xE6EDF3);
    private static final Color ACCENT = new Color(0x6EE7B7);
    private static final Color MUTED = new Color(0x8B949E);

    /** 逻辑更新次数，由游戏线程写入，EDT 读取，所以用 volatile */
    private volatile long ticks;

    /** FPS 统计只在 EDT 上读写，不需要 volatile */
    private long fpsWindowStartNanos = System.nanoTime();
    private int framesInWindow;
    private int fps;

    /** 最近一次按下的键，M2 会用它来改变蛇的方向 */
    private volatile String lastKey = "（还没按过键）";

    public GamePanel() {
        setPreferredSize(new Dimension(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT));
        setBackground(BACKGROUND);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                lastKey = KeyEvent.getKeyText(e.getKeyCode());
            }
        });
    }

    /**
     * 逻辑更新。由 {@link GameLoop} 在游戏线程上按固定节奏调用。
     * M1 阶段只累加计数，M2 会在这里推进蛇的位置。
     */
    public void updateGame() {
        ticks++;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawGrid(g2);
        drawStatus(g2);
        countFrame();
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(GRID);
        int step = GameConfig.TILE_SIZE;
        for (int x = 0; x < getWidth(); x += step) {
            g2.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += step) {
            g2.drawLine(0, y, getWidth(), y);
        }
    }

    private void drawStatus(Graphics2D g2) {
        int x = 24;
        int y = 48;

        g2.setColor(ACCENT);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        g2.drawString("M1 · 窗口 + 固定步长游戏循环", x, y);

        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        g2.setColor(FOREGROUND);
        y += 40;
        g2.drawString("目标帧率 : " + GameConfig.TARGET_FPS + " fps", x, y);
        y += 26;
        g2.drawString("实测帧率 : " + fps + " fps", x, y);
        y += 26;
        g2.drawString("累计更新 : " + ticks, x, y);
        y += 26;
        g2.drawString("最近按键 : " + lastKey, x, y);

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        g2.setColor(MUTED);
        g2.drawString("M1 阶段还没有游戏逻辑，蛇和食物从 M2 开始。", x, getHeight() - 32);
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
