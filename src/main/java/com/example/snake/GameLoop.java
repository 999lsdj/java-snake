package com.example.snake;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * 固定步长（fixed timestep）的游戏循环。
 *
 * <p>它做的事就三件，循环往复：
 * <ol>
 *   <li>算出距离上一帧过了多少时间；</li>
 *   <li>时间攒够一个固定步长（1/60 秒）就调用一次 {@code update}，逻辑更新几次就调用几次；</li>
 *   <li>调用一次 {@code render}，然后把剩余时间睡掉，避免空转把 CPU 占满。</li>
 * </ol>
 *
 * <p>为什么不在每次循环里简单睡 16 毫秒？因为 {@code Thread.sleep} 只能保证"至少睡这么久"，
 * 实际可能是 16 毫秒，也可能是 30 毫秒。睡多久就当过了多久，游戏速度会随电脑负载忽快忽慢。
 * 固定步长把"真实流逝的时间"和"逻辑推进的步数"分开，逻辑永远按固定节奏走。
 *
 * <p>时钟和睡眠都被抽成了接口，所以单元测试能注入假时钟——测试因此可以在几毫秒内
 * 模拟出几十秒的游戏时间，不需要真的等。
 */
public final class GameLoop {

    /** 睡眠动作。抽成接口是为了测试时可替换。 */
    @FunctionalInterface
    public interface Sleeper {
        void sleep(long nanos) throws InterruptedException;
    }

    private final int targetFps;
    private final Runnable update;
    private final Runnable render;
    private final LongSupplier nanoTime;
    private final Sleeper sleeper;

    private volatile boolean running;
    private volatile long updateCount;
    private volatile long frameCount;
    private Thread thread;

    /** 生产环境用的构造器：真实时钟加真实睡眠。 */
    public GameLoop(int targetFps, Runnable update, Runnable render) {
        this(targetFps, update, render, System::nanoTime, GameLoop::sleepNanos);
    }

    /**
     * 完整构造器，供单元测试注入假时钟。
     *
     * @param targetFps 每秒目标帧数，必须大于 0
     * @param update    逻辑更新回调，每过 1/targetFps 秒调用一次
     * @param render    渲染回调，每帧调用一次
     * @param nanoTime  纳秒时钟
     * @param sleeper   睡眠实现
     */
    public GameLoop(int targetFps, Runnable update, Runnable render,
                    LongSupplier nanoTime, Sleeper sleeper) {
        if (targetFps <= 0) {
            throw new IllegalArgumentException("targetFps 必须大于 0，实际收到 " + targetFps);
        }
        this.targetFps = targetFps;
        this.update = Objects.requireNonNull(update, "update 不能为 null");
        this.render = Objects.requireNonNull(render, "render 不能为 null");
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime 不能为 null");
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper 不能为 null");
    }

    /** 开一条后台线程跑循环。只能启动一次。 */
    public void start() {
        if (thread != null) {
            throw new IllegalStateException("游戏循环只能启动一次");
        }
        running = true;
        thread = new Thread(this::runLoop, "game-loop");
        thread.setDaemon(true);
        thread.start();
    }

    /** 请求停止。循环会在当前这帧结束后退出。 */
    public void stop() {
        running = false;
    }

    /** 等待循环真正退出。 */
    public void join() throws InterruptedException {
        if (thread != null) {
            thread.join();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public long updateCount() {
        return updateCount;
    }

    public long frameCount() {
        return frameCount;
    }

    public int targetFps() {
        return targetFps;
    }

    /**
     * 在当前线程上跑循环，跑到 {@link #stop()} 被调用为止。仅供单元测试使用：
     * 测试里用的都是假时钟，不会真的阻塞，所以不需要额外开线程。
     */
    void runOnCurrentThread() {
        running = true;
        runLoop();
    }

    private void runLoop() {
        final long stepNanos = 1_000_000_000L / targetFps;
        long previous = nanoTime.getAsLong();
        long accumulated = 0L;

        while (running) {
            long now = nanoTime.getAsLong();
            accumulated += now - previous;
            previous = now;

            // 攒够一个步长就更新一次逻辑；如果这一帧特别长，就补跑相应的次数
            while (accumulated >= stepNanos) {
                update.run();
                updateCount++;
                accumulated -= stepNanos;
            }

            render.run();
            frameCount++;

            long sleepNanos = stepNanos - accumulated;
            if (sleepNanos > 0) {
                try {
                    sleeper.sleep(sleepNanos);
                } catch (InterruptedException e) {
                    // 被中断说明有人要求我们退出，恢复中断标记后结束循环
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        running = false;
    }

    private static void sleepNanos(long nanos) throws InterruptedException {
        long millis = nanos / 1_000_000L;
        int restNanos = (int) (nanos % 1_000_000L);
        Thread.sleep(millis, restNanos);
    }
}
