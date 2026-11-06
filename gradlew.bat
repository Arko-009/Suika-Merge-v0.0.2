@echo off
setlocal
"C:\Users\USER\.jdks\temurin-21.0.12.1\bin\java.exe" -classpath "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
endlocal