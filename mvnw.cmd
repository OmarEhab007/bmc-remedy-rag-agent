@REM Maven Wrapper Script for Windows
@REM https://github.com/apache/maven-wrapper

@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set MAVEN_WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
set MAVEN_WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties

if not exist "%MAVEN_WRAPPER_JAR%" (
    if not exist "%MAVEN_WRAPPER_PROPERTIES%" (
        echo Error: %MAVEN_WRAPPER_PROPERTIES% not found
        exit /b 1
    )

    for /f "tokens=2 delims==" %%a in ('findstr /b "wrapperUrl=" "%MAVEN_WRAPPER_PROPERTIES%"') do set WRAPPER_URL=%%a

    if defined WRAPPER_URL (
        echo Downloading Maven Wrapper from %WRAPPER_URL%
        powershell -Command "(New-Object Net.WebClient).DownloadFile('%WRAPPER_URL%', '%MAVEN_WRAPPER_JAR%')"
    ) else (
        echo Error: wrapperUrl not found in %MAVEN_WRAPPER_PROPERTIES%
        exit /b 1
    )
)

java %MAVEN_OPTS% %MAVEN_DEBUG_OPTS% -classpath "%MAVEN_WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %MAVEN_CONFIG% %*

endlocal
