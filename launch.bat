@echo off
@title MapleOrigin Launch Version
set PATH=C:\Program Files\Java\jdk1.8.0_111\bin;%PATH%
set CLASSPATH=.;dist\*;cores\*;
java -Xmx2048m -Dwzpath=wz\ net.server.Server
pause