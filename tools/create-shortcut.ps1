<#
.SYNOPSIS
    在你的桌面上创建 java-snake 的快捷方式。

.DESCRIPTION
    为什么快捷方式不直接提交到仓库？因为 .lnk 文件里存的是绝对路径
    （指向你自己的 javaw.exe 和 jar），换一台机器就失效了。
    所以仓库里放的是这个脚本，它在每台机器上按各自的路径现场生成快捷方式。

    脚本会自动找 jar（优先 dist 目录，其次 target 目录）和 javaw.exe
    （优先 JAVA_HOME，其次 PATH，最后翻 Program Files\Java）。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\create-shortcut.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\create-shortcut.ps1 -Name "贪吃蛇" -JarPath D:\games\java-snake-1.0.0.jar
#>
[CmdletBinding()]
param(
    [string]$JarPath,
    [string]$JavaExe,
    [string]$Name = '贪吃蛇',
    [string]$DesktopPath
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

# 一、找 jar
if (-not $JarPath) {
    # 按优先级依次查找：dist 是"稳定部署位置"，target 只是临时产物（mvn clean 会删掉它）。
    # 所以绝不能用"两边一起取最新"的写法——那会把快捷方式指到 target 去。
    $searchDirs = @(
        (Join-Path $repoRoot 'dist'),
        (Join-Path $repoRoot 'target')
    )
    foreach ($dir in $searchDirs) {
        $found = Get-ChildItem -Path $dir -Filter 'java-snake-*.jar' -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($found) {
            $JarPath = $found.FullName
            break
        }
    }
    if (-not $JarPath) {
        throw '找不到 jar。先运行 tools\deploy.ps1（或 mvn package），再运行本脚本。'
    }
}
if (-not (Test-Path -LiteralPath $JarPath)) { throw "找不到 jar：$JarPath" }
$JarPath = (Resolve-Path -LiteralPath $JarPath).Path

# 二、找 javaw.exe（用 javaw 而不是 java，启动时不会弹出黑色命令行窗口）
if (-not $JavaExe) {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += (Join-Path $env:JAVA_HOME 'bin\javaw.exe') }
    $onPath = Get-Command javaw.exe -ErrorAction SilentlyContinue
    if ($onPath) { $candidates += $onPath.Source }
    $candidates += Get-ChildItem -Path 'C:\Program Files\Java' -Filter 'javaw.exe' -Recurse -Depth 3 -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty FullName
    $JavaExe = $candidates | Where-Object { $_ -and (Test-Path -LiteralPath $_) } | Select-Object -First 1
}
if (-not $JavaExe) {
    throw '找不到 javaw.exe。请确认已安装 JDK/JRE，或用 -JavaExe 指定它的完整路径。'
}

# 三、确定桌面位置
if (-not $DesktopPath) { $DesktopPath = [Environment]::GetFolderPath('Desktop') }
if (-not (Test-Path -LiteralPath $DesktopPath)) { throw "找不到桌面目录：$DesktopPath" }

# 四、生成快捷方式
$linkPath = Join-Path $DesktopPath ($Name + '.lnk')
$shell = New-Object -ComObject WScript.Shell
$shortcut = $shell.CreateShortcut($linkPath)
$shortcut.TargetPath = $JavaExe
$shortcut.Arguments = '-jar "' + $JarPath + '"'
$shortcut.WorkingDirectory = Split-Path -Parent $JarPath
$shortcut.Description = 'java-snake 贪吃蛇'
$shortcut.IconLocation = $JavaExe + ',0'
$shortcut.Save()

Write-Output '快捷方式已创建：'
Write-Output ('  位置   : ' + $linkPath)
Write-Output ('  目标   : ' + $JavaExe)
Write-Output ('  参数   : -jar "' + $JarPath + '"')
Write-Output ('  工作目录: ' + (Split-Path -Parent $JarPath))
Write-Output '双击桌面上的图标即可开始游戏（方向键转向，结束后按 R 重开）。'
