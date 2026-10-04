package com.example.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 游戏循环的单元测试。
 *
 * <p>重点看第一个测试：它用的时钟是假的——{@code sleep} 不真的等待，
 * 只是把"当前时间"往后推。所以测试可以在一瞬间跑完 60 帧，不用等 1 秒。
 * 这就是"把时钟抽成接口"换来的好处。
 */
class GameLoopTest {

    /** 假时钟：每次睡眠都把时间往前推，时间流逝完全是模拟出来的。 */
    private static final class VirtualClock implements GameLoop.Sleeper {
        private final AtomicLong now = new AtomicLong();

        @Override
        public void sleep(long nanos) {
            now.addAndGet(nanos);
        }

        long nanoTime() {
            return now.get();
        }
    }

    @Test
    @DisplayName("虚拟时间：经过 60 次逻辑更新后循环按预期停止")
    void stopsAfterExpectedUpdates() {
        VirtualClock clock = new VirtualClock();
        AtomicLong updates = new AtomicLong();

        // lambda 里没法给局部变量赋值，所以用数组做"容器"把循环对象传进回调
        GameLoop[] loopRef = new GameLoop[1];
        GameLoop loop = new GameLoop(60,
                () -> {
                    if (updates.incrementAndGet() == 60) {
                        loopRef[0].stop();
                    }
                },
                () -> { },
                clock::nanoTime,
                clock);
        loopRef[0] = loop;

        loop.runOnCurrentThread();

        assertEquals(60, loop.updateCount(), "应该正好更新 60 次");
        assertTrue(loop.frameCount() >= 60,
                "每帧都要渲染一次，帧数不应该少于更新次数，实际 " + loop.frameCount());
        assertFalse(loop.isRunning(), "循环结束后 running 应该为 false");
    }

    @Test
    @DisplayName("真实时间：start() 之后会推进，stop() 之后会停下")
    void startsAndStopsWithRealClock() throws Exception {
        AtomicLong ticks = new AtomicLong();
        GameLoop loop = new GameLoop(60, ticks::incrementAndGet, () -> { });

        loop.start();
        Thread.sleep(200);
        loop.stop();
        loop.join();

        assertFalse(loop.isRunning(), "stop() 之后循环应该已经退出");
        assertTrue(loop.updateCount() >= 5,
                "200 毫秒按 60fps 至少应有 5 次更新，实际 " + loop.updateCount());
        assertEquals(ticks.get(), loop.updateCount(), "回调次数应与内部计数一致");
    }

    @Test
    @DisplayName("帧率必须大于 0，否则构造时就报错")
    void rejectsNonPositiveFps() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameLoop(0, () -> { }, () -> { }));
        assertThrows(IllegalArgumentException.class,
                () -> new GameLoop(-1, () -> { }, () -> { }));
    }
}
