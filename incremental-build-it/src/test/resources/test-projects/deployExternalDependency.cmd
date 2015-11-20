@echo off
call mvn -f external\pom.xml clean
echo %DATE% %TIME% > external\src\main\resources\timestamp.txt
call mvn -Dmaven.install.skip=true -f external\pom.xml deploy