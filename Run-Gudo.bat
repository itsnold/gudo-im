@echo off
setlocal
cd /d "%~dp0"
set "JAR=lib\mysql-connector-j-9.4.0.jar"

call :findJava
if defined GUDO_JAVA goto driver
if /i not "%~1"=="--setup" goto missingJava
where winget.exe >nul 2>&1
if errorlevel 1 goto missingJava
echo Installing Microsoft OpenJDK 21 with Windows Package Manager...
winget.exe install --id Microsoft.OpenJDK.21 --exact --source winget
if errorlevel 1 goto missingJava
call :findJava
if not defined GUDO_JAVA goto missingJava

:driver

if not exist "%JAR%" (
    echo Downloading MySQL Connector/J 9.4.0 for the first run...
    if not exist lib mkdir lib
    curl.exe --fail --silent --show-error --location --max-time 120 --output "%JAR%.part" "https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/9.4.0/mysql-connector-j-9.4.0.jar"
    if errorlevel 1 (
        del "%JAR%.part" 2>nul
        echo See MYSQL_WORKBENCH_SETUP.md for the manual download.
        goto failure
    )
    move /y "%JAR%.part" "%JAR%" >nul
    if errorlevel 1 goto failure
)

"%GUDO_JAVA%" --class-path "%JAR%" Main.java %*
if errorlevel 1 goto failure
exit /b 0

:findJava
set "GUDO_JAVA="
if defined JAVA_HOME call :checkJava "%JAVA_HOME%\bin\java.exe"
if not defined GUDO_JAVA call :checkJava java.exe
if defined GUDO_JAVA exit /b 0
for /d %%J in ("%ProgramFiles%\Microsoft\jdk-21*") do if not defined GUDO_JAVA call :checkJava "%%~fJ\bin\java.exe"
exit /b 0

:checkJava
("%~1" -XshowSettings:properties -version 2>&1) | findstr /r /c:"java.specification.version = 2[1-9]$" /c:"java.specification.version = [3-9][0-9]$" /c:"java.specification.version = [1-9][0-9][0-9]$" >nul
if not errorlevel 1 set "GUDO_JAVA=%~1"
exit /b 0

:missingJava
echo Install JDK 21 or newer, enable Java on PATH, then retry Setup-Gudo.bat.

:failure
echo.
echo Launch failed. See MYSQL_WORKBENCH_SETUP.md.
pause
exit /b 1
