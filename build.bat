now i can run@echo off
REM Build script for Smart City Recommendation System

echo Building Smart City Recommendation System...
echo.

REM Check if Maven is installed
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven is not installed or not in PATH.
    echo Please install Maven from: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

REM Build the project
echo Running Maven build...
call mvn clean package

if %ERRORLEVEL% EQ 0 (
    echo.
    echo BUILD SUCCESSFUL!
    echo.
    echo To run the application, execute:
    echo java -jar target/SmartCityRecommender.jar
    echo.
) else (
    echo.
    echo BUILD FAILED!
    echo Please check the error messages above.
    pause
    exit /b 1
)

pause
