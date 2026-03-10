#!/bin/bash

# 高速開発環境起動スクリプト
# 用法: ./dev-start.sh

set -e

echo "=========================================="
echo "Vue 3 + Spring Boot 高速開発起動"
echo "=========================================="

# 必要なツールをチェック
check_requirements() {
    echo "環境をチェック中..."
    
    if ! command -v java &> /dev/null; then
        echo "❌ Javaがインストールされていません。JDK 17以上をインストールしてください"
        exit 1
    fi
    
    if ! command -v mvn &> /dev/null; then
        echo "❌ Mavenがインストールされていません。Mavenをインストールしてください"
        exit 1
    fi
    
    if ! command -v node &> /dev/null; then
        echo "❌ Node.jsがインストールされていません。Node.js 18以上をインストールしてください"
        exit 1
    fi
    
    echo "✅ すべての依存関係がインストールされています"
}

# MySQLを起動（Dockerを使用）
start_mysql() {
    echo ""
    echo "MySQLデータベースを起動中..."
    
    if command -v docker &> /dev/null; then
        if docker ps | grep -q aiproject-mysql; then
            echo "✅ MySQLは既に実行中です"
        else
            docker-compose up -d mysql
            echo "✅ MySQLが起動しました"
            sleep 3
        fi
    else
        echo "⚠️  Dockerがインストールされていません。手動でMySQLを起動してください"
        echo "   データベースアドレス: localhost:3306"
        echo "   ユーザー名: root"
        echo "   パスワード: root"
        echo "   データベース: aiproject"
    fi
}

# バックエンドを起動
start_backend() {
    echo ""
    echo "Spring Bootバックエンドを起動中..."
    cd backend
    mvn clean package -DskipTests > /dev/null 2>&1 &
    BACKEND_PID=$!
    echo "✅ バックエンドがバックグラウンドでコンパイル中です... (PID: $BACKEND_PID)"
    echo "   バックエンドURL: http://localhost:8080"
    cd ..
}

# フロントエンドを起動
start_frontend() {
    echo ""
    echo "Vue 3フロントエンドを起動中..."
    cd frontend
    
    # node_modulesをチェック
    if [ ! -d "node_modules" ]; then
        echo "初回実行のため、依存関係をインストール中..."
        npm install > /dev/null 2>&1
    fi
    
    npm run dev > /dev/null 2>&1 &
    FRONTEND_PID=$!
    echo "✅ フロントエンドが起動しました (PID: $FRONTEND_PID)"
    echo "   フロントエンドURL: http://localhost:5173"
    cd ..
}

# 起動情報を表示
show_info() {
    echo ""
    echo "=========================================="
    echo "✅ すべてのサービスが起動しました！"
    echo "=========================================="
    echo ""
    echo "フロントエンド: http://localhost:5173"
    echo "バックエンド:   http://localhost:8080"
    echo "データベース:   localhost:3306"
    echo ""
    echo "Ctrl+C ですべてのサービスを停止します"
    echo "=========================================="
}

# メイン関数
main() {
    check_requirements
    start_mysql
    start_backend
    start_frontend
    show_info
    
    # ユーザーの割り込みを待機
    wait
}

main
