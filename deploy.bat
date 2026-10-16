@echo off
chcp 65001 > nul
cls

echo =======================================================
echo          TW NETWORK - COMPILADOR E ATUALIZADOR         
echo =======================================================
echo.

set "PROJECT_DIR=C:\Users\SUPORTE\IdeaProjects\TW-Project"
set "SERVER_PLUGINS=C:\Users\SUPORTE\Desktop\CoreTech_Server\plugins"

cd /d "%PROJECT_DIR%"

echo [1/2] Compilando modulos com Maven...
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERRO] Falha na compilacao! Os arquivos nao foram copiados.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/2] Atualizando jars no servidor...

:: Modulos
set "MODULOS=tw-core tw-economia tw-lobby tw-maquinas tw-minas tw-ranks tw-spawners tw-terrenos"

for %%M in (%MODULOS%) do (
    if exist "%%M\target\%%M-1.0-SNAPSHOT.jar" (
        copy /y "%%M\target\%%M-1.0-SNAPSHOT.jar" "%SERVER_PLUGINS%\" > nul
        echo  [OK] %%M atualizado.
    ) else (
        echo  [AVISO] Arquivo %%M-1.0-SNAPSHOT.jar nao encontrado em target.
    )
)

echo.
echo =======================================================
echo  Todos os plugins foram copiados com sucesso!
echo =======================================================
echo.
pause