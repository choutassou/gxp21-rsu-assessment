@echo off
setlocal

:: アプリケーションのホームディレクトリを設定
set "APP_HOME=%~dp0.."

:: サービス名
set "SERVICE_NAME=GXP21AmplificationData"

:: サービスの説明
set "SERVICE_DESC=GXP21 Amplification Data Processing Service"

:: Javaの実行パス（必要に応じて変更）
set "JAVA_EXE=java"

:: JVMオプション
set "JAVA_OPTS=-Xms512m -Xmx2g -Dfile.encoding=UTF-8"

:: JARファイルのパス
set "JAR_PATH=%APP_HOME%\lib\gxp21-amplification-data-1.0.0.jar"

:: 作業ディレクトリ
set "WORK_DIR=%APP_HOME%\config"

echo サービスを登録しています...
echo サービス名: %SERVICE_NAME%
echo JAR Path: %JAR_PATH%
echo 作業ディレクトリ: %WORK_DIR%

:: sc コマンドでサービスを作成
sc create "%SERVICE_NAME%" ^
binPath= "\"%JAVA_EXE%\" %JAVA_OPTS% -jar \"%JAR_PATH%\"" ^
start= auto ^
DisplayName= "%SERVICE_DESC%"

if %ERRORLEVEL% EQU 0 (
    echo サービスが正常に登録されました。
    echo.
    echo サービスを開始するには以下のコマンドを実行してください:
    echo sc start %SERVICE_NAME%
    echo.
    echo サービスを停止するには:
    echo sc stop %SERVICE_NAME%
    echo.
    echo サービスを削除するには:
    echo sc delete %SERVICE_NAME%
) else (
    echo サービスの登録に失敗しました。管理者権限で実行してください。
)

pause