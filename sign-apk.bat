@echo off
setlocal

set "JAVA_EXE=C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot\bin\java.exe"
set "SIGNER_JAR=%~dp0uber-apk-signer-1.3.0.jar"

if "%~1"=="" (
    echo.
    echo  ==========================================
    echo   APK Signer - Drag and Drop
    echo  ==========================================
    echo.
    echo  Usage: Drag and drop an APK file onto
    echo         this batch file to sign it.
    echo.
    echo  Or run from command line:
    echo    sign-apk.bat "path\to\your.apk"
    echo.
    pause
    exit /b 1
)

echo.
echo  ==========================================
echo   Signing APK: %~nx1
echo  ==========================================
echo.

"%JAVA_EXE%" -jar "%SIGNER_JAR%" --apks "%~1"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo  SUCCESS! Signed APK is in the same folder
    echo  as the original with "-aligned-debugSigned" suffix.
    echo.
) else (
    echo.
    echo  ERROR: Signing failed. Check the output above.
    echo.
)

pause
