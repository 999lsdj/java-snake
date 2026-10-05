package com.example.snake;

/**
 * 网格上的一个坐标点。
 *
 * <p>col 是列号（从左往右，0 开始），row 是行号（从上往下，0 开始）。
 *
 * <p>这里用 record 而不是普通类：Java 17 的 record 会自动生成构造器、访问器、
 * equals、hashCode 和 toString。后三个正是"把坐标丢进 HashSet 查重"所必需的方法——
 * 用普通类而忘记重写 equals，会出现"明明占了这一格却查不到"的诡异 bug。
 */
public record GridPoint(int col, int row) {

    /** 返回从当前点朝指定方向走一格之后的新坐标（record 不可变，所以是返回新对象） */
    public GridPoint move(Direction direction) {
        return new GridPoint(col + direction.dx(), row + direction.dy());
    }
}
