@echo off
chcp 936 >nul
rem 一键编译签名版 APK，产物复制为“生日提醒-v版本号.apk”（本文件为 GBK 编码，请勿改成 UTF-8）
set "JAVA_HOME=C:\Users\rucha\dev\jdk-17"
cd /d "%~dp0"
call "%~dp0gradlew.bat" :app:testDebugUnitTest :app:assembleRelease --console=plain
if errorlevel 1 (
  echo.
  echo 编译失败，请查看上面的错误信息。
  pause
  exit /b 1
)
for /f "tokens=2 delims==" %%v in ('findstr /c:"versionName = " app\build.gradle.kts') do set "VER=%%~v"
set "VER=%VER: =%"
set "VER=%VER:"=%"
copy /y app\build\outputs\apk\release\app-release.apk "生日提醒-v%VER%.apk" >nul
call "%~dp0gradlew.bat" --stop >nul
echo.
echo 完成：%~dp0生日提醒-v%VER%.apk
pause
