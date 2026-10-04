package com.example.snake;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

/**
 * 程序入口：把窗口和游戏循环装配起来。
 *
 * <p>运行方式：
 * <pre>
 *   java -jar target/java-snake-0.1.0.jar            正常打开窗口
 *   java -cp target/classes com.example.snake.Main --selftest
 * </pre>
 * 自检模式会跑 1.5 秒，打印实际帧数和更新次数后自动退出，用来确认环境正常。
 */
public final class Main {

    private Main() {
        throw new AssertionError("Main 是入口类，不应该被实例化");
    }

    public static void main(String[] args) throws Exception {
        boolean selfTest = Arrays.asList(args).contains("--selftest");

        // Swing 的组件必须在事件分发线程（EDT）上创建，所以这里用 invokeAndWait 把
        // 创建窗口的动作交给 EDT 执行，并等它做完。AtomicReference 只是用来把 EDT
        // 里创建出来的对象带回主线程——lambda 里不能给局部变量赋值。
        AtomicReference<SnakeFrame> frameRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            SnakeFrame frame = new SnakeFrame();
            frame.setVisible(true);
            frameRef.set(frame);
        });

        SnakeFrame frame = frameRef.get();
        GamePanel panel = frame.gamePanel();

        GameLoop loop = new GameLoop(GameConfig.TARGET_FPS, panel::updateGame, panel::repaint);
        loop.start();
        System.out.println("游戏循环已启动，目标帧率 " + GameConfig.TARGET_FPS + " fps");

        if (selfTest) {
            Thread.sleep(1500);
            loop.stop();
            loop.join();
            System.out.println("自检结束：frames=" + loop.frameCount()
                    + " updates=" + loop.updateCount());
            SwingUtilities.invokeAndWait(frame::dispose);
            System.exit(0);
        }
    }
}
