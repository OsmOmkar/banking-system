#!/bin/bash
# ============================================================
# Build script for Java Banking System
# ============================================================

echo "==> Creating output directory..."
mkdir -p out
mkdir -p lib

echo "==> Downloading PostgreSQL JDBC driver..."
if [ ! -f lib/postgresql.jar ]; then
    curl -L "https://jdbc.postgresql.org/download/postgresql-42.7.3.jar" -o lib/postgresql.jar
    echo "    Downloaded postgresql-42.7.3.jar"
fi

echo "==> Compiling Java sources..."
find src/main/java -name "*.java" > sources.txt

javac -source 17 -target 17 -encoding UTF-8 -cp "lib/postgresql.jar" -d out @sources.txt 2>&1
if [ $? -ne 0 ]; then
    echo "ERROR: Compilation failed"
    exit 1
fi

echo "==> Copying resources..."
cp -r src/main/resources/* out/

echo "==> Build complete!"