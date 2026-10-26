@echo off
set "JAVA_HOME=C:\PROGRA~1\Java\jdk-17"
cd /d "%~dp0"
call "%~dp0gradlew.bat" %*
