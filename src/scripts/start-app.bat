@echo off
setlocal

:: アプリケーションのホームディレクトリを設定
set "APP_HOME=%~dp0.."

:: Javaの実行パスを設定（必要に応じて変更）
set "JAVA_EXE=java"

:: JVMオプション
set "JAVA_OPTS=-Xms512m -Xmx2g -Dfile.encoding=UTF-8"

:: ログ設定
set "LOG_CONFIG=-Dlogback.configurationFile=%APP_HOME%\config\logback.xml"

:: 設定ファイルパス
set "CONFIG_FILE=%APP_HOME%\config\Assessment.properties"

:: 作業ディレクトリを設定ファイルのある場所に変更
cd /d "%APP_HOME%\config"

echo Starting GXP21 Amplification Data Application...
echo Config file: %CONFIG_FILE%
echo Working directory: %CD%

:: アプリケーション実行
"%JAVA_EXE%" %JAVA_OPTS% %LOG_CONFIG% -jar "%APP_HOME%\lib\gxp21-amplification-data-1.0.0.jar"

pause