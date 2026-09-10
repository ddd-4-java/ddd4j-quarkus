@echo off
setlocal

set "REPOSITORY_ROOT=%~dp0"
set "MAVEN3_HOME=%REPOSITORY_ROOT%.mvn\maven3-home"
set "GLOBAL_SETTINGS=%MAVEN3_HOME%\conf\settings.xml"

if not exist "%GLOBAL_SETTINGS%" (
  echo Missing Quarkus Maven compatibility settings: %GLOBAL_SETTINGS% 1>&2
  endlocal & exit /b 2
)

call "%REPOSITORY_ROOT%mvnw.cmd" -gs "%GLOBAL_SETTINGS%" "-Dddd4j.maven.home=%MAVEN3_HOME%" %*
set "MAVEN_EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %MAVEN_EXIT_CODE%
