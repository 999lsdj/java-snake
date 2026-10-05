package com.example.snake;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * 蛇的身体。
 *
 * <p>关键约定：**队首是蛇头，队尾是蛇尾**。
 *
 * <p>为什么用 {@link ArrayDeque} 而不是 ArrayList？因为蛇每走一格只做两件事：
 * 在头部插入一个新格子、在尾部删掉一个旧格子。这两件事在双端队列上都是 O(1)；
 * 而在 ArrayList 上，"在头部插入"要把它后面的所有元素整体后移，是 O(n)。
 * 蛇越长越慢，这就是"选对数据结构"的实际价值——比背概念有说服力。
 *
 * <p>{@code body()} 返回的是快照（{@code List.copyOf}），外部拿到列表也改不动蛇，
 * 界面代码就没有机会"顺手"把蛇改坏。
 */
public final class Snake {

    private final Deque<GridPoint> body = new ArrayDeque<>();

    /** 已经吃掉、但还没长出来的长度。攒到下一次移动时生效 */
    private boolean growPending;

    /**
     * 创建一条蛇。
     *
     * @param head   蛇头坐标
     * @param length 初始长度（格数），至少为 1
     * @param facing 初始朝向；身体从这个方向的反方向往后铺开
     */
    public Snake(GridPoint head, int length, Direction facing) {
        if (length < 1) {
            throw new IllegalArgumentException("蛇的长度至少为 1，实际收到 " + length);
        }
        GridPoint current = head;
        for (int i = 0; i < length; i++) {
            body.addLast(current);
            current = new GridPoint(current.col() - facing.dx(), current.row() - facing.dy());
        }
    }

    public GridPoint head() {
        return body.peekFirst();
    }

    public GridPoint tail() {
        return body.peekLast();
    }

    public int length() {
        return body.size();
    }

    /** 身体坐标的快照，顺序是蛇头 → 蛇尾 */
    public List<GridPoint> body() {
        return List.copyOf(body);
    }

    /** 某一格是否被蛇身占据 */
    public boolean occupies(GridPoint point) {
        return body.contains(point);
    }

    /** 标记"下次移动时别砍尾巴"，也就是长一格 */
    public void grow() {
        growPending = true;
    }

    /**
     * 朝指定方向移动一格。
     *
     * <p>先给头部加一格，再决定要不要砍尾巴：正在长身体就保留尾巴（长度 +1），
     * 否则砍掉（长度不变，看起来就是整条蛇往前爬）。
     */
    public void move(Direction direction) {
        body.addFirst(head().move(direction));
        if (growPending) {
            growPending = false;
        } else {
            body.removeLast();
        }
    }
}
