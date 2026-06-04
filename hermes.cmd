@echo off
rem Hermes Agent launcher scoped to Corporate_OA_System project
rem Auto-detects repo root (looks for CLAUDE.md walking up).

setlocal

rem Walk up to find repo root (directory containing CLAUDE.md or .git)
set "REPO_ROOT=%CD%"
:findroot
if exist "%REPO_ROOT%\CLAUDE.md" goto foundroot
if exist "%REPO_ROOT%\.git" goto foundroot
if "%REPO_ROOT%"=="%REPO_ROOT:~0,3%" goto notfound
for %%P in ("%REPO_ROOT%") do set "PARENT=%%~dpP"
set "REPO_ROOT=%PARENT:~0,-1%"
goto findroot
:foundroot

rem Save discovered repo root for downstream scripts
set "HERMES_PROJECT_ROOT=%REPO_ROOT%"
echo [hermes] Project root: %HERMES_PROJECT_ROOT%

set "HERMES_HOME=E:\iex\.hermes"
set "HERMES_INSTALL=%LOCALAPPDATA%\hermes\hermes-agent"
set "HERMES_EXE=%HERMES_INSTALL%\venv\Scripts\hermes.exe"

if not exist "%HERMES_EXE%" (
    echo [ERROR] hermes.exe not found at: %HERMES_EXE%
    echo Re-run installer: powershell -ExecutionPolicy Bypass -File "E:\iex\_hermes-bootstrap\install.ps1" -HermesHome "E:\iex\.hermes" -NonInteractive
    exit /b 1
)

set "PATH=%HERMES_INSTALL%\venv\Scripts;%HERMES_HOME%\bin;%HERMES_HOME%\git\cmd;%HERMES_HOME%\node;%PATH%"
set "HERMES_HOME=%HERMES_HOME%"
set "HERMES_GIT_BASH_PATH=%HERMES_HOME%\git\bin\bash.exe"

rem Run in repo root so file/terminal tools target the project
cd /d "%HERMES_PROJECT_ROOT%"

"%HERMES_EXE%" %*
endlocal
