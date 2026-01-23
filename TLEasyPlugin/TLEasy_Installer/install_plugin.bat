@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo      TLEasy Plugin Installer (User Mode)
echo ========================================================
echo.

REM 1. Locate RegAsm (Standard location for .NET 4.x)
set "REGASM=%WINDIR%\Microsoft.NET\Framework64\v4.0.30319\regasm.exe"

if not exist "!REGASM!" (
    echo ERROR: Could not find RegAsm.exe at:
    echo "!REGASM!"
    echo.
    echo Please ensure .NET Framework 4.8 is installed on this machine.
    pause
    exit /b 1
)

REM 2. Define DLL Path (Looks for DLL in the same folder as this script)
set "DLL_PATH=%~dp0TLEasyPlugin.dll"

if not exist "!DLL_PATH!" (
    echo ERROR: TLEasyPlugin.dll not found!
    echo.
    echo Please make sure you have built the project and copied 
    echo 'TLEasyPlugin.dll' into this folder.
    pause
    exit /b 1
)

REM 3. Register the Plugin
echo Found RegAsm.
echo Registering Plugin...
echo.

REM The /user flag registers it for the Current User (No Admin needed)
REM The /codebase flag tells Windows exactly where the file is located
"!REGASM!" /codebase /user "!DLL_PATH!"

if %errorlevel% equ 0 (
    echo.
    echo ========================================================
    echo [SUCCESS] TLEasy installed successfully!
    echo ========================================================
    echo.
    echo You can now open STK. The plugin should appear in the toolbar.
) else (
    echo.
    echo [FAILED] Registration failed. Error Code: %errorlevel%
)

echo.
pause
