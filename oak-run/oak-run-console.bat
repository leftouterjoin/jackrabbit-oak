set JAR_FILE=%USERPROFILE%\.m2\repository\org\apache\jackrabbit\oak-run\1.1-SNAPSHOT\oak-run-1.1-SNAPSHOT.jar

java -Dfile.encoding=utf-8 -jar %JAR_FILE% console mongodb://127.0.0.1:27017/MongoLuceneIndexPerfPocTest
pause

