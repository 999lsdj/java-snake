package com.example.snake;

/**
 * 最高分的存取。
 *
 * <p>抽成接口的理由和食物生成一样：文件读写是"和外部世界打交道"，很难在测试里控制；
 * 抽出来之后，存档逻辑可以用临时文件测，游戏逻辑则完全不需要碰磁盘。
 */
public interface HighScoreStore {

    /** 读取最高分；没有存档或存档损坏时返回 0 */
    int load();

    /** 保存最高分；写失败不应该让游戏崩溃 */
    void save(int score);

    /** 什么都不做的实现，给"不需要存档"的场景使用 */
    static HighScoreStore disabled() {
        return new HighScoreStore() {
            @Override
            public int load() {
                return 0;
            }

            @Override
            public void save(int score) {
                // 故意留空
            }
        };
    }
}
