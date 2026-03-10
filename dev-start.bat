@echo off
REM 高速開発環境起動スクリプト（Windows版）
REM 用法: dev-start.bat

setlocal enabledelayedexpansion

echo ==========================================
echo Vue 3 + Spring Boot 高速開発起動
echo ==========================================

REM 必要なツールをチェック
echo 環境をチェック中...

java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Javaがインストールされていません。JDK 17以上をインストールしてください
    exit /b 1
)

mvn -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Mavenがインストールされていません。Mavenをインストールしてください
    exit /b 1
)

node --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Node.jsがインストールされていません。Node.js 18以上をインストールしてください
    exit /b 1
)

echo ✅ すべての依存関係がインストールされています

REM MySQLを起動
echo.
echo MySQLデータベースを起動中...

docker ps | find "aiproject-mysql" >nul 2>&1
if errorlevel 1 (
    docker-compose up -d mysql
    echo ✅ MySQLが起動しました
    timeout /t 3 /nobreak
) else (
    echo ✅ MySQLは既に実行中です
)

REM バックエンドを起動
echo.
echo Spring Bootバックエンドを起動中...
cd backend
start "Spring Boot Backend" cmd /k "mvn spring-boot:run"
echo ✅ バックエンドが起動しました (http://localhost:8080)
cd ..

REM フロントエンドを起動
echo.
echo Vue 3フロントエンドを起動中...
cd frontend

REM node_modulesをチェック
if not exist "node_modules" (
    echo 初回実行のため、依存関係をインストール中...
    call npm install
)

start "Vue 3 Frontend" cmd /k "npm run dev"
echo ✅ フロントエンドが起動しました (http://localhost:5173)
cd ..

REM 起動情報を表示
echo.
echo ==========================================
echo ✅ すべてのサービスが起動しました！
echo ==========================================
echo.
echo フロントエンド: http://localhost:5173
echo バックエンド:   http://localhost:8080
echo データベース:   localhost:3306
echo.
echo 任意のキーを押してこのウィンドウを閉じます...
echo ==========================================
pause
