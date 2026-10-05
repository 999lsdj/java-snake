package com.example.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SnakeTest {

    @Test
    @DisplayName("新建的蛇：头在最前，身体朝反方向往后铺开")
    void createsBodyBehindHead() {
        Snake snake = new Snake(new GridPoint(5, 5), 3, Direction.RIGHT);

        assertEquals(3, snake.length());
        assertEquals(new GridPoint(5, 5), snake.head());
        assertEquals(new GridPoint(3, 5), snake.tail());
        assertTrue(snake.occupies(new GridPoint(4, 5)), "中间那格也该被占据");
    }

    @Test
    @DisplayName("移动但不生长：长度不变，头前进一格，尾巴让出一格")
    void moveKeepsLength() {
        Snake snake = new Snake(new GridPoint(5, 5), 3, Direction.RIGHT);

        snake.move(Direction.RIGHT);

        assertEquals(3, snake.length());
        assertEquals(new GridPoint(6, 5), snake.head());
        assertEquals(new GridPoint(4, 5), snake.tail());
        assertFalse(snake.occupies(new GridPoint(3, 5)), "旧尾巴应该空出来");
    }

    @Test
    @DisplayName("生长：长度加一，尾巴留在原地")
    void growAddsOneCell() {
        Snake snake = new Snake(new GridPoint(5, 5), 3, Direction.RIGHT);

        snake.grow();
        snake.move(Direction.RIGHT);

        assertEquals(4, snake.length());
        assertEquals(new GridPoint(6, 5), snake.head());
        assertEquals(new GridPoint(3, 5), snake.tail(), "长身体时尾巴不动");
    }

    @Test
    @DisplayName("body() 返回快照，外部改不动蛇")
    void bodyIsSnapshot() {
        Snake snake = new Snake(new GridPoint(5, 5), 2, Direction.RIGHT);
        List<GridPoint> snapshot = snake.body();

        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.add(new GridPoint(9, 9)));
        assertEquals(2, snake.length(), "外部拿到的是副本，蛇本身没变");
    }

    @Test
    @DisplayName("长度小于 1 直接报错")
    void rejectsZeroLength() {
        assertThrows(IllegalArgumentException.class,
                () -> new Snake(new GridPoint(1, 1), 0, Direction.RIGHT));
    }
}
