@echo off
REM MySQL 初始化脚本 (Windows Batch)
REM 这个脚本会执行 SQL 文件来创建表和插入数据

setlocal enabledelayedexpansion

echo 正在连接 MySQL 数据库...
echo 数据库主机: localhost
echo 数据库名: aiproject
echo 用户名: root
echo.

REM 尝试查找 MySQL 安装路径
for /f "tokens=*" %%a in ('powershell -NoProfile -Command "Get-ItemProperty 'HKLM:\Software\MySQL AB\MySQL Server 8.0' 'Location' -EA SilentlyContinue | select -exp Location"') do set "MYSQL_PATH=%%a"

if defined MYSQL_PATH (
    echo 找到 MySQL: !MYSQL_PATH!
    "!MYSQL_PATH!\bin\mysql.exe" -h localhost -u root -proot aiproject < init-mysql.sql
) else (
    echo 未找到 MySQL 命令行工具
    echo.
    echo 请手动执行以下步骤:
    echo 1. 打开 MySQL Workbench
    echo 2. 连接到您的 MySQL 数据库
    echo 3. 文件 - 打开 SQL 脚本: init-mysql.sql
    echo 4. 执行该脚本
    pause
)
