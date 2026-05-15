@echo off
title Fix Maven PATH — MediBook Setup
color 0B

echo.
echo  =====================================================
echo   Maven PATH Fixer — MediBook Setup
echo  =====================================================
echo.
echo  This script will find Maven on your computer and
echo  add it to your system PATH permanently.
echo.
echo  After running this, you NEVER need to worry about
echo  "mvn is not recognized" again.
echo.

:: ── Check if mvn is already working ─────────────────────────────────────────
where mvn >nul 2>&1
if %errorlevel%==0 (
    echo  [OK] Maven is already in your PATH! Nothing to fix.
    echo.
    mvn --version
    echo.
    pause
    exit /b 0
)

echo  [INFO] Searching for Maven on your computer...
echo.

set FOUND_PATH=

:: Check STS4 bundled Maven
for /d %%D in ("%USERPROFILE%\.sts4\*") do (
    for /d %%P in ("%%D\plugins\org.apache.maven.*") do (
        if exist "%%P\bin\mvn.cmd" (
            set FOUND_PATH=%%P\bin
            echo  [FOUND] STS Maven at: %%P\bin
            goto :found
        )
    )
)

:: Check Eclipse bundled Maven
for /d %%D in ("%USERPROFILE%\.eclipse\*") do (
    for /d %%P in ("%%D\plugins\org.apache.maven.*") do (
        if exist "%%P\bin\mvn.cmd" (
            set FOUND_PATH=%%P\bin
            echo  [FOUND] Eclipse Maven at: %%P\bin
            goto :found
        )
    )
)

:: Common paths
for %%M in (
    "C:\maven\bin"
    "C:\maven3\bin"
    "C:\Program Files\Maven\bin"
    "C:\Program Files\apache-maven\bin"
    "C:\tools\maven\bin"
    "D:\maven\bin"
    "D:\apache-maven\bin"
    "D:\tools\maven\bin"
) do (
    if exist "%%~M\mvn.cmd" (
        set FOUND_PATH=%%~M
        echo  [FOUND] Maven at: %%~M
        goto :found
    )
)

:: Not found anywhere
color 0C
echo  [NOT FOUND] Maven is not installed on this computer.
echo.
echo  Please install Maven:
echo    1. Go to: https://maven.apache.org/download.cgi
echo    2. Download "Binary zip archive" (apache-maven-3.x.x-bin.zip)
echo    3. Extract to C:\maven
echo    4. Run this script again — it will auto-add C:\maven\bin to PATH
echo.
pause
exit /b 1

:found
echo.
echo  Adding "%FOUND_PATH%" to your system PATH...
echo.

:: Add to user PATH using setx (no admin needed)
setx PATH "%PATH%;%FOUND_PATH%" > nul 2>&1
if %errorlevel%==0 (
    echo  [SUCCESS] Maven PATH added!
    echo.
    echo  =====================================================
    echo   IMPORTANT — You must restart your terminal for
    echo   the change to take effect. Then run start-all.bat
    echo  =====================================================
) else (
    echo  [INFO] Trying machine-level PATH (needs Admin)...
    :: Try setting machine-level PATH
    for /f "tokens=2*" %%A in ('reg query "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v PATH') do set SYS_PATH=%%B
    setx PATH "%SYS_PATH%;%FOUND_PATH%" /M > nul 2>&1
    if %errorlevel%==0 (
        echo  [SUCCESS] Maven PATH added at system level!
    ) else (
        echo.
        echo  [MANUAL] Could not auto-add. Add it manually:
        echo    1. Press Win+R → type "sysdm.cpl" → Enter
        echo    2. Advanced tab → Environment Variables
        echo    3. Under "User variables" → select PATH → Edit
        echo    4. Click New → paste: %FOUND_PATH%
        echo    5. Click OK on all windows
        echo    6. Restart your terminal
    )
)

echo.
pause
