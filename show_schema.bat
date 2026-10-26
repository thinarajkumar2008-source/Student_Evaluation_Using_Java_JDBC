@echo off
cd /d "%~dp0"
javac -cp ".;lib/*" ShowDatabaseSchema.java >nul 2>&1
java -cp ".;lib/*" ShowDatabaseSchema
pause
