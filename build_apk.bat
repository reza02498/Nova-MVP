@echo off
setlocal
title Nova MVP - Build APK

echo.
echo ============================================
echo   Nova MVP - APK Builder v1.0
echo ============================================
echo.

:: ============================================
:: STEP 0: Find Java 17
:: ============================================
echo [1/6] Looking for Java 17...

set JAVA_FOUND=0

:: Check common install locations
for %%D in (
    "C:\Program Files\Java\jdk-17"
    "C:\Program Files\Eclipse Adoptium\jdk-17"
    "C:\Program Files\Microsoft\jdk-17"
    "C:\Program Files\OpenJDK\jdk-17"
) do (
    if exist %%D\bin\javac.exe (
        set JAVA_HOME=%%D
        set JAVA_FOUND=1
        echo   OK Found: %%D
    )
)

:: Check wildcard matching for Adoptium versioned paths
if %JAVA_FOUND%==0 (
    for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do (
        if exist "%%D\bin\javac.exe" (
            set "JAVA_HOME=%%D"
            set JAVA_FOUND=1
            echo   OK Found: %%D
        )
    )
)

if %JAVA_FOUND%==1 goto :java_ok

echo.
echo   ERROR: Java 17 not found.
echo.
echo   Please install Java 17 from:
echo     https://adoptium.net/download/
echo   Version: 17, JVM: HotSpot, Windows x64
echo   After install, run this script again.
echo.
pause
exit /b 1

:java_ok
"%JAVA_HOME%\bin\java" -version 2>&1 | findstr /i "version"
echo.

:: ============================================
:: STEP 1: Setup Android SDK directories
:: ============================================
echo [2/6] Setting up Android SDK...

set SDK_ROOT=%USERPROFILE%\Android\SDK
if not exist "%SDK_ROOT%" mkdir "%SDK_ROOT%"

:: ============================================
:: STEP 2: Install cmdline-tools
:: ============================================
if exist "%SDK_ROOT%\cmdline-tools\latest\bin\sdkmanager.bat" goto :sdk_ready

set CMDLINE_ZIP=%~dp0cmdline-tools.zip

:: Check if user already downloaded the file manually
if exist "%CMDLINE_ZIP%" (
    echo   Using local cmdline-tools.zip found in project folder.
    goto :extract_sdk
)

echo.
echo   Android SDK Command-line Tools not found.
echo   Google servers may be blocked from your location.
echo.
echo   MANUAL STEP NEEDED:
echo   1. Download this file with your browser (use VPN if needed):
echo      https://dl.google.com/android/repository/commandlinetools-win-14742923_latest.zip
echo   2. Save it to: %CMDLINE_ZIP%
echo   3. Run this script again.
echo.
echo   Trying automatic download anyway...
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; try { Invoke-WebRequest -Uri 'https://dl.google.com/android/repository/commandlinetools-win-14742923_latest.zip' -OutFile '%CMDLINE_ZIP%' } catch { exit 1 }}"
if %ERRORLEVEL% NEQ 0 (
    echo   Automatic download failed (expected in Iran).
    echo   Please follow the manual step above.
    pause
    exit /b 1
)

:extract_sdk
if not exist "%CMDLINE_ZIP%" (
    echo   ERROR: cmdline-tools.zip not found.
    pause
    exit /b 1
)

echo   Extracting SDK tools...
if not exist "%SDK_ROOT%\cmdline-tools\latest" mkdir "%SDK_ROOT%\cmdline-tools\latest"
powershell -Command "Expand-Archive -Path '%CMDLINE_ZIP%' -DestinationPath '%SDK_ROOT%\cmdline-tools\latest' -Force"

:: Handle nested directory structure
if exist "%SDK_ROOT%\cmdline-tools\latest\cmdline-tools\bin\sdkmanager.bat" (
    xcopy /E /Y "%SDK_ROOT%\cmdline-tools\latest\cmdline-tools\*" "%SDK_ROOT%\cmdline-tools\latest\" >nul
    rmdir /s /q "%SDK_ROOT%\cmdline-tools\latest\cmdline-tools" 2>nul
)

:: ZIP no longer needed - but keep it in project folder for reference
:: del "%CMDLINE_ZIP%" 2>nul

echo   OK SDK tools installed
echo.

:sdk_ready
set SDKMANAGER=%SDK_ROOT%\cmdline-tools\latest\bin\sdkmanager.bat

:: ============================================
:: STEP 3: Install SDK packages from LOCAL files
:: ============================================
echo [3/6] Installing SDK packages from local files...

set PACKAGES_DIR=%~dp0sdk-packages

if not exist "%PACKAGES_DIR%" (
    echo   ERROR: sdk-packages folder missing. Create: %PACKAGES_DIR%
    echo   Put these files inside:
    echo     - platform-35_r01.zip
    echo     - build-tools_r35_windows.zip
    echo     - platform-tools-latest-windows.zip
    pause
    exit /b 1
)

:: Extract platform-35 to android-35 folder
if exist "%PACKAGES_DIR%\platform-35_r01.zip" (
    echo   Extracting platform-35...
    if exist "%SDK_ROOT%\platforms\android-35" rmdir /s /q "%SDK_ROOT%\platforms\android-35"
    mkdir "%SDK_ROOT%\platforms\android-35"
    powershell -Command "Expand-Archive -Path '%PACKAGES_DIR%\platform-35_r01.zip' -DestinationPath '%SDK_ROOT%\platforms\android-35' -Force"
    :: Fix nested folder: if android-35/android-35/ exists, move contents up
    if exist "%SDK_ROOT%\platforms\android-35\android-35\android.jar" (
        powershell -Command "Move-Item -Path '%SDK_ROOT%\platforms\android-35\android-35\*' -Destination '%SDK_ROOT%\platforms\android-35\' -Force; Remove-Item -Path '%SDK_ROOT%\platforms\android-35\android-35' -Force"
    )
    echo   OK Platform 35
)

:: Extract build-tools to 35.0.0 folder
if exist "%PACKAGES_DIR%\build-tools_r35_windows.zip" (
    echo   Extracting build-tools...
    if exist "%SDK_ROOT%\build-tools\35.0.0" rmdir /s /q "%SDK_ROOT%\build-tools\35.0.0"
    mkdir "%SDK_ROOT%\build-tools\35.0.0"
    powershell -Command "Expand-Archive -Path '%PACKAGES_DIR%\build-tools_r35_windows.zip' -DestinationPath '%SDK_ROOT%\build-tools\35.0.0' -Force"
    :: Fix nested folder: any subdirectory containing aapt.exe → move contents up
    powershell -Command "$dir = Get-ChildItem -Path '%SDK_ROOT%\build-tools\35.0.0' -Directory | Where-Object { Test-Path (Join-Path $_.FullName 'aapt.exe') } | Select-Object -First 1; if ($dir) { Get-ChildItem -Path $dir.FullName | Move-Item -Destination '%SDK_ROOT%\build-tools\35.0.0\' -Force; Remove-Item -Path $dir.FullName -Force }"
    echo   OK Build-tools 35
)

:: Extract platform-tools
if exist "%PACKAGES_DIR%\platform-tools-latest-windows.zip" (
    echo   Extracting platform-tools...
    if exist "%SDK_ROOT%\platform-tools" rmdir /s /q "%SDK_ROOT%\platform-tools"
    mkdir "%SDK_ROOT%\platform-tools"
    powershell -Command "Expand-Archive -Path '%PACKAGES_DIR%\platform-tools-latest-windows.zip' -DestinationPath '%SDK_ROOT%\platform-tools' -Force"
    :: Fix nested folder: if platform-tools/platform-tools/ exists, move contents up
    if exist "%SDK_ROOT%\platform-tools\platform-tools\adb.exe" (
        powershell -Command "Move-Item -Path '%SDK_ROOT%\platform-tools\platform-tools\*' -Destination '%SDK_ROOT%\platform-tools\' -Force; Remove-Item -Path '%SDK_ROOT%\platform-tools\platform-tools' -Force"
    )
    echo   OK Platform-tools
)

:: Verify all packages
set VERIFY_OK=1
if not exist "%SDK_ROOT%\platforms\android-35\android.jar" (
    echo   ERROR: Platform 35 - android.jar not found
    set VERIFY_OK=0
)
if not exist "%SDK_ROOT%\build-tools\35.0.0\aapt.exe" (
    echo   ERROR: Build-tools 35 - aapt.exe not found
    set VERIFY_OK=0
)
if not exist "%SDK_ROOT%\platform-tools\adb.exe" (
    echo   ERROR: Platform-tools - adb.exe not found
    set VERIFY_OK=0
)
if %VERIFY_OK%==0 (
    pause
    exit /b 1
)
echo   OK All SDK packages installed and verified
echo.

:: ============================================
:: STEP 4: Write local.properties
:: ============================================
echo [4/6] Configuring project...

:: Need to escape backslashes for properties file
set SDK_PATH=%SDK_ROOT:\=\\%
echo sdk.dir=%SDK_PATH%> "%~dp0local.properties"

:: ============================================
:: STEP 5: Setup Gradle wrapper
:: ============================================
echo [5/6] Setting up Gradle...

set WRAPPER_DIR=%~dp0gradle\wrapper
if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"

:: Write gradle-wrapper.properties
(
echo distributionBase=GRADLE_USER_HOME
echo distributionPath=wrapper/dists
echo distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
echo networkTimeout=10000
echo validateDistributionUrl=true
echo zipStoreBase=GRADLE_USER_HOME
echo zipStorePath=wrapper/dists
) > "%WRAPPER_DIR%\gradle-wrapper.properties"

:: Download gradle-wrapper.jar if needed
if not exist "%WRAPPER_DIR%\gradle-wrapper.jar" (
    echo   Downloading Gradle wrapper...
    powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar' -OutFile '%WRAPPER_DIR%\gradle-wrapper.jar'}"
)

if not exist "%WRAPPER_DIR%\gradle-wrapper.jar" (
    echo   ERROR: Could not download gradle-wrapper.jar
    pause
    exit /b 1
)

:: Create gradlew.bat
(
echo @echo off
echo set DIRNAME=%%~dp0
echo if "%%DIRNAME%%"=="" set DIRNAME=.
echo set APP_HOME=%%DIRNAME%%
echo set JAVA_HOME=%JAVA_HOME%
echo set CLASSPATH=%%APP_HOME%%gradle\wrapper\gradle-wrapper.jar
echo "%%JAVA_HOME%%\bin\java" %%DEFAULT_JVM_OPTS%% %%JAVA_OPTS%% -classpath "%%CLASSPATH%%" org.gradle.wrapper.GradleWrapperMain %%*
) > "%~dp0gradlew.bat"

echo   OK Gradle ready
echo.

:: ============================================
:: STEP 6: BUILD!
:: ============================================
echo [6/6] Building APK...
echo   (First build may take 5-15 minutes - downloading dependencies)
echo.

cd /d "%~dp0"
call "%~dp0gradlew.bat" assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ============================================
    echo   BUILD FAILED - check errors above
    echo ============================================
    echo.
    pause
    exit /b 1
)

:: ============================================
:: SUCCESS
:: ============================================
set APK=%~dp0app\build\outputs\apk\debug\app-debug.apk

if exist "%APK%" (
    for %%A in ("%APK%") do set APK_SIZE=%%~zA
    set /a APK_MB=APK_SIZE/1048576

    echo.
    echo ============================================
    echo   BUILD SUCCESS!
    echo.
    echo   APK: %APK%
    echo   Size: %APK_SIZE% bytes
    echo.
    echo   Transfer this file to your phone and install.
    echo ============================================
    echo.

    :: Open folder with APK selected
    explorer /select,"%APK%"
) else (
    echo.
    echo ============================================
    echo   APK not found at expected location.
    echo   Check: app\build\outputs\apk\
    echo ============================================
)

echo.
pause
endlocal
