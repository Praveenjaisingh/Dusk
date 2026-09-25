
setlocal

set DB_NAME=marketplace_db
set DB_USER=root
set DB_PASS=Crevel@123

echo This will DROP and recreate database '%DB_NAME%'. All data will be lost.
set /p CONFIRM=Continue? [y/N]:
if /i not "%CONFIRM%"=="y" (
    echo Cancelled.
    exit /b 0
)

echo Dropping and recreating '%DB_NAME%'...
postgresql -u %DB_USER% -p%DB_PASS% -e "DROP DATABASE IF EXISTS `%DB_NAME%`; CREATE DATABASE `%DB_NAME%`;"

echo Starting the app — Hibernate will recreate the schema, then DataSeeder will populate sample data...
cd /d "%~dp0.."
mvn spring-boot:run

endlocal
