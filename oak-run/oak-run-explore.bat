if "%1"=="" ( 
	@echo �G���[�F�������w�肳��Ă��܂��� 
	pause
	exit
)

set STORE_PATH=%1
set JAR_FILE=%USERPROFILE%\.m2\repository\org\apache\jackrabbit\oak-run\1.1-SNAPSHOT\oak-run-1.1-SNAPSHOT.jar

start /b java -Dfile.encoding=utf-8 -jar %JAR_FILE% explore %STORE_PATH%