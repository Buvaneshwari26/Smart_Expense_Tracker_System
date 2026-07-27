$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'
$env:MAVEN_HOME = 'C:\Users\Buvaneshwari\Downloads\apache-maven-3.9.14-bin\apache-maven-3.9.14'
Remove-Item Env:\*MAVEN_HOME* -ErrorAction SilentlyContinue
$env:MAVEN_HOME = 'C:\Users\Buvaneshwari\Downloads\apache-maven-3.9.14-bin\apache-maven-3.9.14'
& "$env:JAVA_HOME\bin\java.exe" -classpath "$env:MAVEN_HOME\boot\plexus-classworlds-2.9.0.jar" "-Dclassworlds.conf=$env:MAVEN_HOME\bin\m2.conf" "-Dmaven.home=$env:MAVEN_HOME" "-Dmaven.multiModuleProjectDirectory=C:\Users\Buvaneshwari\.gemini\antigravity\scratch\smart-expense-tracker" org.codehaus.plexus.classworlds.launcher.Launcher clean spring-boot:run
