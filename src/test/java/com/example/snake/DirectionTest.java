package com.example.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DirectionTest {

    @Test
    @DisplayName("位移量：屏幕坐标 y 轴朝下，所以\"上\"是 -1")
    void displacement() {
        assertEquals(0, Direction.UP.dx());
        assertEquals(-1, Direction.UP.dy());
        assertEquals(0, Direction.DOWN.dx());
        assertEquals(1, Direction.DOWN.dy());
        assertEquals(-1, Direction.LEFT.dx());
        assertEquals(0, Direction.LEFT.dy());
        assertEquals(1, Direction.RIGHT.dx());
        assertEquals(0, Direction.RIGHT.dy());
    }

    @Test
    @DisplayName("相反方向：上下互为相反，左右互为相反，两个方向都判得出来")
    void opposites() {
        assertTrue(Direction.UP.isOpposite(Direction.DOWN));
        assertTrue(Direction.DOWN.isOpposite(Direction.UP));
        assertTrue(Direction.LEFT.isOpposite(Direction.RIGHT));
        assertTrue(Direction.RIGHT.isOpposite(Direction.LEFT));
    }

    @Test
    @DisplayName("垂直方向、自身、null 都不算相反")
    void nonOpposites() {
        assertFalse(Direction.UP.isOpposite(Direction.LEFT));
        assertFalse(Direction.LEFT.isOpposite(Direction.UP));
        assertFalse(Direction.DOWN.isOpposite(Direction.RIGHT));
        assertFalse(Direction.UP.isOpposite(Direction.UP));
        assertFalse(Direction.RIGHT.isOpposite(null));
    }
}
