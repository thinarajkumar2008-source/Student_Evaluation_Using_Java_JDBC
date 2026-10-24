@echo off
echo =========================================================================
echo Compiling Kongu Engineering College (R2024) Student Evaluation System...
echo =========================================================================
javac -cp ".;lib/*" -d . *.java
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Compilation completed successfully!
) else (
    echo [ERROR] Compilation failed.
)
pause
