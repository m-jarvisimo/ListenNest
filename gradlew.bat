@echo off
setlocal
set APP_HOME=%~dp0
set LOCAL_GRADLE=C:\opt\k2s\.tooling\gradle-8.7\bin\gradle.bat
if exist "%LOCAL_GRADLE%" (
  call "%LOCAL_GRADLE%" %*
  exit /b %ERRORLEVEL%
)
if defined JAVA_HOME (
  set JAVA_CMD=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_CMD=java.exe
)

"%JAVA_CMD%" -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar;%APP_HOME%gradle\wrapper\gradle-wrapper-shared.jar;%APP_HOME%gradle\wrapper\gradle-cli.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
