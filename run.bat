@echo off
if not defined JAVA_HOME (
    set "JAVA_HOME=C:\Program Files\Apache NetBeans\jdk"
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo =========================================================================
echo              AeroFleet Autonomous Drone Simulation Platform
echo =========================================================================
echo Starting Spring Boot Server on http://localhost:8080 ...
echo H2 Console available at: http://localhost:8080/h2-console
echo =========================================================================

call "%~dp0mvnw.cmd" spring-boot:run
pause
