@echo off
setlocal

:: �A�v���P�[�V�����̃z�[���f�B���N�g����ݒ�
set "APP_HOME=%~dp0.."

:: Java�̎��s�p�X��ݒ�i�K�v�ɉ����ĕύX�j
set "JAVA_EXE=java"

:: JVM�I�v�V����
set "JAVA_OPTS=-Xms512m -Xmx2g -Dfile.encoding=UTF-8"

:: ���O�ݒ�
set "LOG_CONFIG=-Dlogback.configurationFile=%APP_HOME%\config\logback.xml"

:: �ݒ�t�@�C���p�X
set "CONFIG_FILE=%APP_HOME%\config\Assessment.properties"

:: ��ƃf�B���N�g����ݒ�t�@�C���̂���ꏊ�ɕύX
cd /d "%APP_HOME%\config"

echo Starting GXP21 Amplification Data Application...
echo Config file: %CONFIG_FILE%
echo Working directory: %CD%

:: �A�v���P�[�V�������s
"%JAVA_EXE%" %JAVA_OPTS% %LOG_CONFIG% -jar "%APP_HOME%\lib\gxp21-amplification-data-1.0.0.jar"

pause