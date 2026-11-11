@echo off
title MycoNet - Build and Deploy
echo ========================================================
echo             MYCONET OFF-GRID MESH DEPLOY
echo ========================================================
echo.

set ADB="C:\Users\Administrator\AppData\Local\Android\Sdk\platform-tools\adb.exe"

if not exist %ADB% (
    echo [EROARE] Nu am gasit adb.exe in %ADB%!
    pause
    exit /b 1
)

echo [1/3] Compilare Frontend WebApp Svelte + Tailwind...
cd /d "%~dp0web"
call npm run build
if %ERRORLEVEL% NEQ 0 (
    echo [EROARE] Compilarea WebApp a esuat!
    pause
    exit /b 1
)

echo.
echo [2/3] Compilare APK Nativ Android (Kotlin + Protobuf)...
cd /d "%~dp0android"
call gradlew.bat assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo [EROARE] Compilarea APK-ului a esuat!
    pause
    exit /b 1
)

echo.
echo [3/3] Instalare automata pe telefonul conectat prin USB...
%ADB% install -r -g "%~dp0android\app\build\outputs\apk\debug\app-debug.apk"
%ADB% shell "monkey -p net.myconet.mesh -c android.intent.category.LAUNCHER 1"

echo.
echo ========================================================
echo   [SUCCES] MycoNet a fost instalat si pornit pe telefon!
echo ========================================================
pause
