@echo off
chcp 65001 > nul
set JAVA="C:\Program Files\Java\jdk-21\bin\java.exe"
set BIN=C:\10000hcode)))))\Java\BaithiTH\bin
set CP=%BIN%;C:\10000hcode)))))\Java\BaithiTH\libs\commons-collections-3.2.2.jar;C:\10000hcode)))))\Java\BaithiTH\libs\commons-collections4-4.4.jar;C:\10000hcode)))))\Java\BaithiTH\libs\commons-compress-1.21.jar;C:\10000hcode)))))\Java\BaithiTH\libs\commons-io-2.11.0.jar;C:\10000hcode)))))\Java\BaithiTH\libs\commons-math3-3.6.1.jar;C:\10000hcode)))))\Java\BaithiTH\libs\curvesapi-1.07.jar;C:\10000hcode)))))\Java\BaithiTH\libs\log4j-1.2.17.jar;C:\10000hcode)))))\Java\BaithiTH\libs\log4j-api-2.18.0.jar;C:\10000hcode)))))\Java\BaithiTH\libs\poi-5.2.3.jar;C:\10000hcode)))))\Java\BaithiTH\libs\poi-ooxml-5.2.3.jar;C:\10000hcode)))))\Java\BaithiTH\libs\poi-ooxml-lite-5.2.3.jar;C:\10000hcode)))))\Java\BaithiTH\libs\xmlbeans-5.1.1.jar
echo Dang chay ServerApp...
%JAVA% -cp "%CP%" ServerApp
pause
