@echo off
setlocal

rem Use CALL so control returns after running mvn.cmd (a batch file)
call mvn -q -P mc-1.21.7 -DskipTests package || goto :fail
call mvn -q -P mc-1.21.8 -DskipTests package || goto :fail

echo.
echo Build finished. Artifacts in target\:
for %%F in ("target\*-mc-*.jar") do echo   %%~nxF
exit /b 0

:fail
echo Build failed with exit code %errorlevel%
exit /b %errorlevel%