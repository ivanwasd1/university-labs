#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d out @sources.txt
java -Dfile.encoding=UTF-8 -cp out Main "$@"
