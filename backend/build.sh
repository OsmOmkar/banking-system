#!/bin/bash
# ============================================================
# Build script for Java Banking System
# ============================================================

echo "==> Creating output directory..."
mkdir -p out

echo "==> Downloading PostgreSQL JDBC driver..."
mkdir -p lib
if [ ! -f lib/postgresql.jar ]; then
    curl -L "https://jdbc.postgresql.org/download/postgresql-42.7.3.jar" -o lib/postgresql.jar
    echo "    Downloaded postgresql-42.7.3.jar"
fi

echo "==> Downloading JavaMail libraries..."
if [ ! -f lib/javax.mail.jar ]; then
    curl -L "https://repo1.maven.org/maven2/com/sun/mail/javax.mail/1.6.2/javax.mail-1.6.2.jar" -o lib/javax.mail.jar
    echo "    Downloaded javax.mail-1.6.2.jar"
fi
if [ ! -f lib/activation.jar ]; then
    curl -L "https://repo1.maven.org/maven2/javax/activation/activation/1.1.1/activation-1.1.1.jar" -o lib/activation.jar
    echo "    Downloaded activation-1.1.1.jar"
fi

echo "==> Compiling Java sources..."
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 -cp "lib/postgresql.jar:lib/javax.mail.jar:lib/activation.jar" -d out @sources.txt

if [ $? -ne 0 ]; then
    echo "ERROR: Compilation failed"
    exit 1
fi

echo "==> Copying resources..."
cp -r src/main/resources/* out/

echo "==> Build complete!"
