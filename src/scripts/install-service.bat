@echo off
setlocal

:: �A�v���P�[�V�����̃z�[���f�B���N�g����ݒ�
set "APP_HOME=%~dp0.."

:: �T�[�r�X��
set "SERVICE_NAME=GXP21AmplificationData"

:: �T�[�r�X�̐���
set "SERVICE_DESC=GXP21 Amplification Data Processing Service"

:: Java�̎��s�p�X�i�K�v�ɉ����ĕύX�j
set "JAVA_EXE=java"

:: JVM�I�v�V����
set "JAVA_OPTS=-Xms512m -Xmx2g -Dfile.encoding=UTF-8"

:: JAR�t�@�C���̃p�X
set "JAR_PATH=%APP_HOME%\lib\gxp21-amplification-data-1.0.0.jar"

:: ��ƃf�B���N�g��
set "WORK_DIR=%APP_HOME%\config"

echo �T�[�r�X��o�^���Ă��܂�...
echo �T�[�r�X��: %SERVICE_NAME%
echo JAR Path: %JAR_PATH%
echo ��ƃf�B���N�g��: %WORK_DIR%

:: sc �R�}���h�ŃT�[�r�X���쐬
sc create "%SERVICE_NAME%" ^
binPath= "\"%JAVA_EXE%\" %JAVA_OPTS% -jar \"%JAR_PATH%\"" ^
start= auto ^
DisplayName= "%SERVICE_DESC%"

if %ERRORLEVEL% EQU 0 (
    echo �T�[�r�X������ɓo�^����܂����B
    echo.
    echo �T�[�r�X���J�n����ɂ͈ȉ��̃R�}���h�����s���Ă�������:
    echo sc start %SERVICE_NAME%
    echo.
    echo �T�[�r�X���~����ɂ�:
    echo sc stop %SERVICE_NAME%
    echo.
    echo �T�[�r�X���폜����ɂ�:
    echo sc delete %SERVICE_NAME%
) else (
    echo �T�[�r�X�̓o�^�Ɏ��s���܂����B�Ǘ��Ҍ����Ŏ��s���Ă��������B
)

pause