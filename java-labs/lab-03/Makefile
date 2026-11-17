.DEFAULT_GOAL := build
.PHONY: clean build run test
.NOTPARALLEL:

ifeq ($(OS),Windows_NT)
SHELL := cmd.exe
.SHELLFLAGS := /c
SEP := ;
MKDIR := if not exist build mkdir build
CLEAN := powershell -NoProfile -Command "Get-ChildItem build/*.class,build/*.jar -ErrorAction SilentlyContinue | Remove-Item -Force"
else
SEP := :
MKDIR := mkdir -p build
CLEAN := rm -f build/*.class build/*.jar
endif

clean:
	$(CLEAN)

build: clean
	$(MKDIR)
	javac -encoding UTF-8 -source 11 -target 11 -Xlint:-options -cp "lib/*" -d build src/*.java
	jar cfe build/quadrilaterals.jar Main -C build .

run:
	java -ea -Dfile.encoding=UTF-8 -jar build/quadrilaterals.jar

test: build
	java -ea -Dfile.encoding=UTF-8 -cp "build$(SEP)lib/*" QuadrilateralTest
