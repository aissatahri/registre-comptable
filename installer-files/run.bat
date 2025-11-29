@echo off
setlocal
set APPDIR=%~dp0
rem If a runtime is bundled under runtime\bin\javaw.exe use it, otherwise fallback to system javaw
if exist "%APPDIR%runtime\bin\javaw.exe" (
    "%APPDIR%runtime\bin\javaw.exe" -jar "%APPDIR%registre-comptable-1.0.0-all.jar"
) else (
    javaw -jar "%APPDIR%registre-comptable-1.0.0-all.jar"
)
