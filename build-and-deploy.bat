@echo off
setlocal
title TW Plugin - Build e Deploy

set "PROJECT_DIR=%~dp0"
set "SERVER_DIR=%PROJECT_DIR%..\TW_Network_Lastest"
set "SERVER_PLUGINS=%SERVER_DIR%\plugins"
set "MODULES=tw-core tw-economia tw-maquinas tw-minas tw-ranks tw-spawners tw-terrenos tw-farms tw-caixas"

echo ==========================================
echo       TW PLUGIN - BUILD E DEPLOY
echo ==========================================
echo.

if not exist "%SERVER_PLUGINS%\" (
    echo [ERRO] Pasta de plugins nao encontrada: "%SERVER_PLUGINS%"
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "$serverDir=[IO.Path]::GetFullPath('%SERVER_DIR%'); $file=Join-Path $serverDir 'server.properties'; if (!(Test-Path -LiteralPath $file)) { exit 2 }; $line=Get-Content -LiteralPath $file | Where-Object { $_ -match '^server-port=' } | Select-Object -First 1; $port=25565; if ($line) { try { $port=[int]($line -replace '^server-port=','') } catch { exit 2 } }; try { $pattern='^\s*TCP\s+\S+:' + [regex]::Escape([string]$port) + '\s+\S+\s+LISTENING\s+(\d+)\s*$'; $connections=@(netstat.exe -ano -p tcp | Select-String -Pattern $pattern); if ($LASTEXITCODE -ne 0) { throw 'netstat retornou erro' } } catch { Write-Output 'Nao foi possivel consultar a porta do servidor.'; exit 4 }; if (!$connections) { exit 0 }; $serverJars=@(Get-ChildItem -LiteralPath $serverDir -Filter '*.jar' -File -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name); $runningServer=$false; foreach ($connection in $connections) { if ($connection.Line -notmatch '\s+(\d+)\s*$') { continue }; $listenerPid=[int]$Matches[1]; $process=Get-CimInstance -ClassName Win32_Process -Filter ('ProcessId = ' + $listenerPid) -ErrorAction SilentlyContinue; if (!$process) { Write-Output ('PID ' + $listenerPid + ' esta escutando na porta ' + $port + ', mas nao foi possivel identificar o processo.'); continue }; $commandLine=[string]$process.CommandLine; $jarMatch=$false; foreach ($jar in $serverJars) { if ($commandLine.IndexOf($jar,[StringComparison]::OrdinalIgnoreCase) -ge 0) { $jarMatch=$true; break } }; if (($process.Name -match '^(java|javaw)\.exe$') -and (($commandLine.IndexOf($serverDir,[StringComparison]::OrdinalIgnoreCase) -ge 0) -or $jarMatch)) { Write-Output ('Servidor Java deste diretorio ainda esta ativo (PID ' + $process.ProcessId + ').'); $runningServer=$true; break }; Write-Output ('Processo usando a porta ' + $port + ': ' + $process.Name + ' (PID ' + $process.ProcessId + ').'); Write-Output $commandLine }; if ($runningServer) { exit 1 }; exit 3"
if errorlevel 4 (
    echo [ERRO] Nao foi possivel verificar a porta do servidor. Confirme manualmente e tente novamente.
    pause
    exit /b 4
)
if errorlevel 3 (
    echo [AVISO] A porta esta ocupada, mas o processo nao foi identificado como este servidor.
    choice /C SN /N /M "Se confirmou que o TW-Network esta parado, continuar? (S=sim, N=nao) "
    if errorlevel 2 (
        echo Deploy cancelado.
        pause
        exit /b 1
    )
    goto :server_check_passed
)
if errorlevel 2 (
    echo [ERRO] Nao encontrei server.properties para verificar se o servidor esta ligado.
    pause
    exit /b 2
)
if errorlevel 1 (
    echo [ERRO] O servidor TW-Network ainda parece estar ligado. Pare-o antes de atualizar os plugins.
    pause
    exit /b 1
)

:server_check_passed
for /f %%I in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmmss"') do set "STAMP=%%I"
set "STAGE_DIR=%SERVER_DIR%\temp\tw-plugin-stage-%STAMP%"
set "BACKUP_DIR=%SERVER_DIR%\backups\tw-plugin-%STAMP%"

pushd "%PROJECT_DIR%"
if errorlevel 1 (
    echo [ERRO] Nao foi possivel acessar a pasta do projeto.
    pause
    exit /b 1
)

echo [1/3] Compilando todos os modulos e executando os testes...
call mvn -U clean package
if errorlevel 1 goto :build_failed

echo.
echo [2/3] Preparando e validando os arquivos...
if not exist "%STAGE_DIR%\" mkdir "%STAGE_DIR%"
if errorlevel 1 goto :stage_failed

for %%M in (%MODULES%) do (
    if not exist "%PROJECT_DIR%%%M\target\%%M-1.0-SNAPSHOT.jar" (
        echo [ERRO] Artefato de build ausente: %%M-1.0-SNAPSHOT.jar
        goto :stage_failed
    )
    copy /y "%PROJECT_DIR%%%M\target\%%M-1.0-SNAPSHOT.jar" "%STAGE_DIR%\" > nul
    if errorlevel 1 goto :stage_failed
    if not exist "%STAGE_DIR%\%%M-1.0-SNAPSHOT.jar" (
        echo [ERRO] Artefato obrigatorio ausente: %%M-1.0-SNAPSHOT.jar
        goto :stage_failed
    )
)

echo.
echo [3/3] Fazendo backup dos plugins atuais e instalando os novos...
mkdir "%BACKUP_DIR%"
if errorlevel 1 goto :stage_failed
for %%M in (%MODULES%) do (
    if exist "%SERVER_PLUGINS%\%%M-1.0-SNAPSHOT.jar" (
        copy /y "%SERVER_PLUGINS%\%%M-1.0-SNAPSHOT.jar" "%BACKUP_DIR%\" > nul
        if errorlevel 1 goto :copy_failed
    )
)

for %%M in (%MODULES%) do (
    copy /y "%STAGE_DIR%\%%M-1.0-SNAPSHOT.jar" "%SERVER_PLUGINS%\" > nul
    if errorlevel 1 goto :copy_failed
)

rmdir /s /q "%STAGE_DIR%"
popd
echo.
echo [OK] Build, testes, backup e deploy concluidos.
echo Backup anterior: "%BACKUP_DIR%"
echo Inicie o servidor para carregar os plugins atualizados.
pause
exit /b 0

:build_failed
set "RESULT=%ERRORLEVEL%"
popd
echo.
echo [ERRO] Build ou testes falharam. Nenhum plugin foi copiado.
pause
exit /b %RESULT%

:stage_failed
set "RESULT=%ERRORLEVEL%"
if exist "%STAGE_DIR%\" rmdir /s /q "%STAGE_DIR%"
popd
echo.
echo [ERRO] Nao foi possivel preparar/validar os JARs. Plugins atuais preservados.
pause
exit /b %RESULT%

:copy_failed
echo [ERRO] A copia falhou. Restaurando o backup...
for %%M in (%MODULES%) do (
    if exist "%BACKUP_DIR%\%%M-1.0-SNAPSHOT.jar" (
        copy /y "%BACKUP_DIR%\%%M-1.0-SNAPSHOT.jar" "%SERVER_PLUGINS%\" > nul
    ) else (
        if exist "%SERVER_PLUGINS%\%%M-1.0-SNAPSHOT.jar" del /q "%SERVER_PLUGINS%\%%M-1.0-SNAPSHOT.jar"
    )
)
if exist "%STAGE_DIR%\" rmdir /s /q "%STAGE_DIR%"
popd
echo.
echo [ERRO] Deploy incompleto; os arquivos anteriores foram restaurados.
pause
exit /b 1
