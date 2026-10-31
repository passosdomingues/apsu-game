@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0jogar-windows.ps1"
if errorlevel 1 (
  echo.
  echo Nao foi possivel abrir As Aguas de Apsu. Consulte a mensagem acima.
  pause
  exit /b 1
)
