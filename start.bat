@echo off
chcp 65001 >nul
title Token Monitor Server

echo ================================
echo    ⚡ Token Monitor
echo    正在启动服务器...
echo ================================
echo.
echo 浏览器打开: http://localhost:8000
echo 按 Ctrl+C 可以关闭服务器
echo ================================
echo.

cd /d "%~dp0"
python server.py

pause
