package com.example.snake;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 存档的测试。
 *
 * <p>{@code @TempDir} 让 JUnit 在系统临时目录里建一个专属文件夹，
 * 测试往里写文件不会污染真实环境，跑完自动清理。
 */
class FileHighScoreStoreTest {

    @Test
    @DisplayName("没有存档文件时读出 0")
    void missingFileReadsZero(@TempDir Path dir) {
        HighScoreStore store = new FileHighScoreStore(dir.resolve("nested/highscore.txt"));
        assertEquals(0, store.load());
    }

    @Test
    @DisplayName("存进去再读出来是同一个数字，并且会自动创建目录")
    void savesAndLoads(@TempDir Path dir) {
        Path file = dir.resolve("nested/highscore.txt");
        HighScoreStore store = new FileHighScoreStore(file);

        store.save(42);

        assertEquals(42, store.load());
        assertEquals(42, new FileHighScoreStore(file).load(), "换一个实例也应读到同一个值");
    }

    @Test
    @DisplayName("存档内容被写坏时读出 0，且不抛异常")
    void corruptedFileReadsZero(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("highscore.txt");
        Files.writeString(file, "这不是一个数字", StandardCharsets.UTF_8);

        assertEquals(0, new FileHighScoreStore(file).load());
    }

    @Test
    @DisplayName("存档位置不可写时，保存失败但不抛异常")
    void saveFailureIsSilent(@TempDir Path dir) throws IOException {
        Path blocked = dir.resolve("blocked");
        Files.createFile(blocked);                       // 先用一个文件占住这个名字
        Path impossible = blocked.resolve("highscore.txt");   // 父路径是文件，建目录必然失败

        new FileHighScoreStore(impossible).save(99);      // 不应抛出异常
    }
}
