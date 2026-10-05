<#
.SYNOPSIS
    打包 java-snake 并把 jar 复制到 dist 目录。

.DESCRIPTION
    dist 目录是"给快捷方式和启动脚本用的固定位置"。target 目录会被 mvn clean 删掉，
    所以不能把快捷方式指向那里。

    需要 PATH 里能找到 mvn。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\deploy.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\deploy.ps1 -SkipTests
#>
[CmdletBinding()]
param(
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

$mvn = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if (-not $mvn) { $mvn = Get-Command mvn -ErrorAction SilentlyContinue }
if (-not $mvn) { throw 'PATH 里找不到 mvn，请先确认 Maven 已安装并加入 PATH。' }

$mvnArgs = @('clean', 'package')
if ($SkipTests) { $mvnArgs += '-DskipTests' }

Push-Location $repoRoot
try {
    & $mvn.Source @mvnArgs
    if ($LASTEXITCODE -ne 0) { throw "mvn 执行失败，退出码 $LASTEXITCODE" }

    $jar = Get-ChildItem -Path (Join-Path $repoRoot 'target') -Filter 'java-snake-*.jar' |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $jar) { throw 'target 目录里没有找到 jar。' }

    $dist = Join-Path $repoRoot 'dist'
    New-Item -ItemType Directory -Force -Path $dist | Out-Null

    # 清掉 dist 里的旧版本 jar，只留刚部署的这个。
    # 路径先经过检查，确保删除范围不会跑出 dist 目录。
    $distFull = [IO.Path]::GetFullPath($dist) + [IO.Path]::DirectorySeparatorChar
    Get-ChildItem -Path $dist -Filter 'java-snake-*.jar' -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -ne $jar.Name } |
        ForEach-Object {
            $full = [IO.Path]::GetFullPath($_.FullName)
            if (-not $full.StartsWith($distFull, [StringComparison]::OrdinalIgnoreCase)) {
                throw "拒绝删除 dist 目录之外的路径：$full"
            }
            Remove-Item -LiteralPath $full -Force
            Write-Output ("已清理旧版本: " + $_.Name)
        }

    Copy-Item -LiteralPath $jar.FullName -Destination $dist -Force

    $deployed = Join-Path $dist $jar.Name
    Write-Output ("已部署: " + $deployed)
    Write-Output ("SHA256: " + (Get-FileHash -LiteralPath $deployed -Algorithm SHA256).Hash)
    Write-Output '下一步可以运行 tools\create-shortcut.ps1 在桌面创建快捷方式。'
}
finally {
    Pop-Location
}
