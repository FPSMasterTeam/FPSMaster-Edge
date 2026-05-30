@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

@rem Download gradle-wrapper.jar if not present
if not exist "%CLASSPATH%" goto download

:run
@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%GRADLE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega

goto :EOF

@rem --------------------------------------------------------------------------
@rem Download gradle-wrapper.jar using PowerShell (bypasses SSL certificate errors)
@rem --------------------------------------------------------------------------
:download
echo.
echo gradle-wrapper.jar not found, downloading...
echo.

set GRADLE_VERSION=8.6
set WRAPPER_JAR_URL=https://raw.githubusercontent.com/gradle/gradle/v%GRADLE_VERSION%/gradle/wrapper/gradle-wrapper.jar
set WRAPPER_JAR_DIR=%APP_HOME%\gradle\wrapper

echo Attempting to download gradle-wrapper.jar...
echo.

REM Try PowerShell with SSL callback bypass (handles certificate errors in China)
PowerShell -NoProfile -ExecutionPolicy Bypass -Command ^
    "[System.Net.ServicePointManager]::ServerCertificateValidationCallback = {$true};" ^
    "[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12;" ^
    "try { " ^
        "$wc = New-Object System.Net.WebClient;" ^
        "$wc.DownloadFile('%WRAPPER_JAR_URL%', '%CLASSPATH%');" ^
        "Write-Host 'Downloaded gradle-wrapper.jar successfully'; " ^
        "exit 0; " ^
    "} catch { " ^
        "Write-Host 'Failed to download gradle-wrapper.jar: ' + $_.Exception.Message; " ^
        "Write-Host 'You may need to manually place gradle-wrapper.jar in %WRAPPER_JAR_DIR%'; " ^
        "exit 1; " ^
    "}"

if %ERRORLEVEL% neq 0 (
    echo.
    echo WARNING: Could not download gradle-wrapper.jar due to SSL/network issues.
    echo You can manually download it from:
    echo   https://github.com/gradle/gradle/raw/v%GRADLE_VERSION%/gradle/wrapper/gradle-wrapper.jar
    echo And place it in: %WRAPPER_JAR_DIR%
    echo.
    echo Alternatively, run this script again after setting JAVA_HOME properly,
    echo or use a VPN/proxy if you are behind a restricted network.
    goto fail
)

if exist "%CLASSPATH%" (
    echo gradle-wrapper.jar is ready.
    goto run
) else (
    echo Failed to download gradle-wrapper.jar.
    goto fail
)
