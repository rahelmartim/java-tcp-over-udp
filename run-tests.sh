#!/usr/bin/env sh
# Compiles the code and the tests, and runs all the tests.
# Usage: ./run-tests.sh [filter], for example ./run-tests.sh EndToEnd
set -eu
cd "$(dirname "$0")"
rm -rf out
mkdir -p out
javac -encoding UTF-8 -d out src/*.java test/*.java
# Short timers make the timer tests fast. The end-to-end tests set their own.
java -Dtcpow.tempo.estouro=200 -Dtcpow.tempo.servidor=600 -Dtcpow.classes=out -cp out TestRunner "${1:-}"
