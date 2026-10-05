package com.example.snake;

import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.Random;

import javax.swing.JFrame;

/**
 * 主窗口。
 *
 * <p>这个类只做三件事：设置标题、把画布放进去、按画布的首选尺寸定窗口大小。
 * 把"窗口"和"游戏逻辑"分开，是为了让逻辑部分能在没有图形界面的环境里被测试。
 */
public final class SnakeFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    /** 最高分存到用户主目录下的隐藏文件夹里 */
    private static final Path HIGH_SCORE_PATH =
            Path.of(System.getProperty("user.home"), ".java-snake", "highscore.txt");

    private final HighScoreStore highScoreStore = new FileHighScoreStore(HIGH_SCORE_PATH);

    /** 游戏状态放在窗口里，画布只负责显示与输入 */
    private final SnakeGame game = new SnakeGame(
            GameConfig.COLS,
            GameConfig.ROWS,
            GameConfig.INITIAL_MOVES_PER_SECOND,
            GameConfig.TARGET_FPS,
            GameConfig.INITIAL_SNAKE_LENGTH,
            FoodSpawner.random(new Random()),
            ObstacleLayout.random(GameConfig.OBSTACLE_COUNT, new Random()));

    private final GamePanel gamePanel = new GamePanel(game, highScoreStore);

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
