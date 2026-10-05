# java-snake

用 Java 17 + Swing 写的贪吃蛇。这是第一个正式项目，目标是走完一遍完整的开发流程：
需求 → 设计 → 编码 → 测试 → 打包 → 发布。

## 运行画面

![M3 运行画面](docs/screenshot-m3.png)

800×600 窗口。顶部状态栏显示分数、蛇的长度、当前方向和实测帧率；橙色圆点是食物，正方形的分节是蛇。
（图中蛇已经吃到 10 个食物、长度 13。截图由项目自己的绘制代码离屏渲染生成，数字是真实跑出来的。
M1、M2 的画面分别见 `docs/screenshot-m1.png`、`docs/screenshot-m2.png`。）

## 当前进度

| 里程碑 | 内容 | 状态 |
| --- | --- | --- |
| M1 | Swing 窗口 + 固定步长游戏循环 | 已完成 |
| M2 | 蛇的移动、键盘控制、撞墙判定 | 已完成 |
| M3 | 食物、成长、分数、自撞判定 | 已完成 |
| M4 | 结束画面、R 键快速重开、打包发布 | 进行中 |

## 环境要求

- JDK 17
- Maven 3.9.16

## 运行

```
mvn clean package
java -jar target/java-snake-0.1.0.jar
```

开发期想快速跑一遍，不想打包：

```
mvn -q compile
java -cp target/classes com.example.snake.Main
```

自检模式（跑 1.5 秒后自动输出帧数并退出，用于确认环境没问题）：

```
java -cp target/classes com.example.snake.Main --selftest
```

## 测试

```
mvn test
```

## 代码结构

| 文件 | 职责 |
| --- | --- |
| `Main.java` | 程序入口，装配窗口与游戏循环 |
| `SnakeFrame.java` | 主窗口 |
| `GamePanel.java` | 画布：绘制网格、蛇、状态栏，接收方向键 |
| `SnakeGame.java` | 游戏状态：蛇、方向、速度换算、撞墙判定（纯逻辑） |
| `FoodSpawner.java` | 食物生成策略（可替换，便于测试） |
| `Snake.java` | 蛇的身体，头进尾出 |
| `Direction.java` | 四个方向及各自的位移量 |
| `GridPoint.java` | 网格坐标（record） |
| `GameLoop.java` | 固定步长游戏循环（纯逻辑，不依赖 Swing） |
| `GameConfig.java` | 窗口尺寸、帧率等常量 |

设计上有一条硬规则：**游戏循环不能依赖 Swing**。这样它才能在单元测试里被验证，
将来换渲染方式（比如换成 JavaFX）也不用重写。

## 讲解卡

- [M1 讲解卡](docs/M1-讲解卡.md)
- [M2 讲解卡](docs/M2-讲解卡.md)
- [M3 讲解卡](docs/M3-讲解卡.md)

## 许可

MIT
