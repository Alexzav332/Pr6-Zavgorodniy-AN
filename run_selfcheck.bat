@echo off
chcp 65001 >nul
if exist out rmdir /s /q out
mkdir out
for /r src %f in (*.java) do echo %f>>sources.txt
javac -encoding UTF-8 -d out @sources.txt
del sources.txt
java -Dfile.encoding=UTF-8 -cp out ru.edu.pr03.SelfCheck %*
