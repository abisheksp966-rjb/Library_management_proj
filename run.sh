#!/usr/bin/env bash
# ==============================================================================
# Library Book Lending and Usage Analysis System (Scenario 4)
# Velalar College of Engineering and Technology, Erode - 638012
# Student: Abishek | Register No: 732925ITR004 | Section: 25IT3A
# ==============================================================================

echo "=============================================================================="
echo "   VELALAR COLLEGE OF ENGINEERING AND TECHNOLOGY, ERODE - 638012"
echo "             DEPARTMENT OF INFORMATION TECHNOLOGY"
echo "   LIBRARY BOOK LENDING AND USAGE ANALYSIS SYSTEM (SCENARIO 4)"
echo "   Student: Abishek | Register No: 732925ITR004 | Section: 25IT3A"
echo "=============================================================================="
echo ""

MODE="$1"

if [ "$MODE" == "--cli" ]; then
    echo "Compiling Java CLI..."
    mkdir -p bin
    javac -encoding UTF-8 -d bin src/com/library/model/*.java src/com/library/exception/*.java src/com/library/util/*.java src/com/library/dao/*.java src/com/library/service/*.java src/com/library/server/*.java src/com/library/Main.java
    java -cp "bin:lib/*" com.library.Main
    exit 0
fi

if [ "$MODE" == "--python" ]; then
    python3 python_analytics/analysis.py
    exit 0
fi

# Default: Start Web Server
if command -v java &> /dev/null; then
    echo "[INFO] Java detected. Compiling and starting Java Web Server..."
    mkdir -p bin
    javac -encoding UTF-8 -d bin src/com/library/model/*.java src/com/library/exception/*.java src/com/library/util/*.java src/com/library/dao/*.java src/com/library/service/*.java src/com/library/server/*.java src/com/library/Main.java
    echo "Starting server on http://localhost:8080 ..."
    java -cp "bin:lib/*" com.library.server.LibraryServer 8080
elif command -v node &> /dev/null; then
    echo "[INFO] Node.js detected. Starting Web Server on http://localhost:8080 ..."
    node server.js
else
    echo "[ERROR] Neither Java nor Node.js was found in PATH."
    exit 1
fi
