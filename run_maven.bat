@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-25
set MAVEN_HOME=C:\Users\Buvaneshwari\Downloads\apache-maven-3.9.14-bin\apache-maven-3.9.14
"%JAVA_HOME%\bin\java.exe" -classpath "%MAVEN_HOME%\boot\*" -Dclassworlds.conf="%MAVEN_HOME%\bin\m2.conf" -Dmaven.home="%MAVEN_HOME%" -Dmaven.multiModuleProjectDirectory="C:\Users\Buvaneshwari\.gemini\antigravity\scratch\smart-expense-tracker" org.codehaus.plexus.classworlds.launcher.Launcher clean spring-boot:run
