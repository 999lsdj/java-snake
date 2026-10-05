<#
.SYNOPSIS
    用 jpackage 生成"免安装版"（自带 Java 运行时），并打包成 zip。

.DESCRIPTION
    朋友拿到 zip 解压后双击 java-snake.exe 就能玩，**不需要自己安装 Java**。
    代价是体积变大（解压后约 60 MB，压缩后约 22 MB），而且只能在本平台运行。

    需要 JDK 17+（jpackage 是 JDK 自带的工具）。生成 msi 安装包才需要额外的 WiX Toolset，
    本脚本走的是 app-image 路线，不需要 WiX。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\build-app-image.ps1
#>
[CmdletBinding()]
param(
    [string]$JarPath,
    [switch]$SkipZip
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$buildDir = Join-Path $repoRoot 'build'
$distDir = Join-Path $repoRoot 'dist'

<#
    只允许删除 build 目录内部的内容。
    递归删除是这套脚本里最危险的动作，所以先做一次路径检查：
    万一将来有人把变量改错，也不会误删到仓库之外的东西。
#>
function Remove-InsideBuild {
    param([Parameter(Mandatory = $true)][string]$Path)
    $full = [IO.Path]::GetFullPath($Path)
    $root = [IO.Path]::GetFullPath($buildDir) + [IO.Path]::DirectorySeparatorChar
    if (-not $full.StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) {
        throw "拒绝删除 build 目录之外的路径：$full"
    }
    if (Test-Path -LiteralPath $full) {
        Remove-Item -LiteralPath $full -Recurse -Force
    }
}

# 一、找 jar：优先 dist（稳定位置），其次 target
if (-not $JarPath) {
    foreach ($dir in @($distDir, (Join-Path $repoRoot 'target'))) {
        $found = Get-ChildItem -Path $dir -Filter 'java-snake-*.jar' -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($found) { $JarPath = $found.FullName; break }
    }
}
if (-not $JarPath -or -not (Test-Path -LiteralPath $JarPath)) {
    throw '找不到 jar。先运行 tools\deploy.ps1（或 mvn package）。'
}
$JarPath = (Resolve-Path -LiteralPath $JarPath).Path
$jarName = Split-Path -Leaf $JarPath
if ($jarName -notmatch '^java-snake-(.+)\.jar$') {
    throw "jar 名字不符合 java-snake-<版本>.jar 的约定：$jarName"
}
$version = $Matches[1]

# 二、找 jpackage
$jpackage = $null
if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
    if (Test-Path -LiteralPath $candidate) { $jpackage = $candidate }
}
if (-not $jpackage) {
    $onPath = Get-Command jpackage.exe -ErrorAction SilentlyContinue
    if ($onPath) { $jpackage = $onPath.Source }
}
if (-not $jpackage) {
    $jpackage = Get-ChildItem -Path 'C:\Program Files\Java' -Filter 'jpackage.exe' -Recurse -Depth 3 -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $jpackage) {
    throw '找不到 jpackage.exe。它随 JDK 一起安装，请确认装的是 JDK 而不是只有 JRE。'
}

# 三、把 jar 单独放进一个干净的输入目录（jpackage 会把这个目录整个拷进应用里）
New-Item -ItemType Directory -Force -Path $buildDir | Out-Null
$inputDir = Join-Path $buildDir 'input'
Remove-InsideBuild -Path $inputDir
New-Item -ItemType Directory -Force -Path $inputDir | Out-Null
Copy-Item -LiteralPath $JarPath -Destination $inputDir -Force

# 四、生成免安装版
Remove-InsideBuild -Path (Join-Path $buildDir 'java-snake')
& $jpackage --type app-image `
    --name java-snake `
    --input $inputDir `
    --main-jar $jarName `
    --main-class com.example.snake.Main `
    --app-version $version `
    --add-modules java.base,java.desktop `
    --dest $buildDir
if ($LASTEXITCODE -ne 0) { throw "jpackage 执行失败，退出码 $LASTEXITCODE" }

$imageDir = Join-Path $buildDir 'java-snake'
$exe = Join-Path $imageDir 'java-snake.exe'
if (-not (Test-Path -LiteralPath $exe)) { throw "jpackage 没有产出预期的 exe：$exe" }
$sizeMb = [Math]::Round(((Get-ChildItem $imageDir -Recurse -File | Measure-Object Length -Sum).Sum / 1MB), 1)
Write-Output ("免安装版已生成: " + $exe + "（解压后约 " + $sizeMb + " MB）")

# 五、打成 zip 方便分享
if (-not $SkipZip) {
    New-Item -ItemType Directory -Force -Path $distDir | Out-Null
    $zip = Join-Path $distDir ("java-snake-" + $version + "-windows-x64.zip")
    if (Test-Path -LiteralPath $zip) { Remove-Item -LiteralPath $zip -Force }
    Compress-Archive -Path $imageDir -DestinationPath $zip -CompressionLevel Optimal
    Write-Output ("压缩包: " + $zip)
    Write-Output ("大小  : " + [Math]::Round((Get-Item -LiteralPath $zip).Length / 1MB, 1) + " MB")
    Write-Output ("SHA256: " + (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash)
}
