@echo off
setlocal
cd /d "%~dp0"
set "JAR=lib\mysql-connector-j-9.4.0.jar"

where java >nul 2>&1
if errorlevel 1 (
    echo Install JDK 21 and enable Java on PATH first.
    goto failure
)

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

java --class-path "%JAR%" Main.java
if errorlevel 1 goto failure
exit /b 0

:failure
echo.
echo Launch failed. See MYSQL_WORKBENCH_SETUP.md.
pause
exit /b 1
