@echo off
set JAVA_HOME=
gradlew.bat assembleDebug
exit /b %ERRORLEVEL%
