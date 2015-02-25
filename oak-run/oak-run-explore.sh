#!/bin/sh

if [ $# -eq 0 ]; then
    echo "エラー：引数が指定されていません " 1>&2
    exit 1
fi

STORE_PATH=$1
JAR_FILE=~/.m2/repository/org/apache/jackrabbit/oak-run/1.1-SNAPSHOT/oak-run-1.1-SNAPSHOT.jar

java -Dfile.encoding=utf-8 -jar $JAR_FILE explore $STORE_PATH &

