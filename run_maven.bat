@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-25"
set "MAVEN_HOME=C:\Users\Buvaneshwari\Downloads\apache-maven-3.9.14-bin\apache-maven-3.9.14"
set "MAVEN_SKIP_RC=1"
call "%MAVEN_HOME%\bin\mvn.cmd" %*
