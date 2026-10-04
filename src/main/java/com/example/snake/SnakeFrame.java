package com.example.snake;

import java.awt.BorderLayout;

import javax.swing.JFrame;

/**
 * 主窗口。
 *
 * <p>这个类只做三件事：设置标题、把画布放进去、按画布的首选尺寸定窗口大小。
 * 把"窗口"和"游戏逻辑"分开，是为了让逻辑部分能在没有图形界面的环境里被测试。
 */
public final class SnakeFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final GamePanel gamePanel = new GamePanel();

    public SnakeFrame() {
        super(GameConfig.TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        add(gamePanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }

    public GamePanel gamePanel() {
        return gamePanel;
    }
}
