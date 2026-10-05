package com.example.snake;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 把最高分存成一个纯文本文件（里面只有一个数字）。
 *
 * <p>两个刻意的设计：
 * <ol>
 *   <li><b>任何一步出错都吞掉异常</b>。存档失败、文件被占用、内容是乱码——
 *       这些都不该让游戏崩溃或弹错误框。最高分丢了是小事，游戏打不开是大事。</li>
 *   <li>读取时用 {@code Integer.parseInt} 并捕获 {@code NumberFormatException}，
 *       所以就算文件被别的程序写坏了，也只是回到 0 分重新开始记录。</li>
 * </ol>
 */
public final class FileHighScoreStore implements HighScoreStore {

    private final Path path;

    public FileHighScoreStore(Path path) {
        this.path = path;
    }

    @Override
    public int load() {
        try {
            if (!Files.exists(path)) {
                return 0;
            }
            String text = Files.readString(path, StandardCharsets.UTF_8).trim();
            return Math.max(0, Integer.parseInt(text));
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void save(int score) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, Integer.toString(score), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // 写不进去就算了，游戏继续
        }
    }

    /** 存档文件路径，方便在界面上或日志里告诉玩家存到了哪里 */
    public Path path() {
        return path;
    }
}
