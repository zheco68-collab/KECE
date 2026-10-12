@echo off
@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script
@REM ----------------------------------------------------------------------------

setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@REM Find java executable
if defined JAVA_HOME (
  set JAVA_EXE="%JAVA_HOME%\bin\java.exe"
) else (
  set JAVA_EXE=java
)

if not exist %JAVA_EXE% (
  echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
  exit /b 1
)

if not exist %WRAPPER_JAR% (
  echo ERROR: Maven wrapper jar not found at %WRAPPER_JAR%
  exit /b 1
)

%JAVA_EXE% %MAVEN_OPTS% "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" -classpath %WRAPPER_JAR% %WRAPPER_LAUNCHER% %*
set EXIT_CODE=%ERRORLEVEL%

exit /b %EXIT_CODE%
