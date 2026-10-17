# Operação dos plugins TW

## Build e deploy

Execute `build-and-deploy.bat` com o servidor parado. O script compila e testa o reactor, prepara os oito JARs em uma pasta temporária, faz backup dos JARs atuais em `TW-Network/backups` e instala os artefatos. Se a cópia falhar, tenta restaurar o backup.

## PostgreSQL

Configure `TW_DB_HOST`, `TW_DB_PORT`, `TW_DB_DATABASE`, `TW_DB_USERNAME` e `TW_DB_PASSWORD`, ou preencha os campos equivalentes em `plugins/tw-core/config.yml`. Não coloque credenciais reais em arquivos versionados.

O Core registra as versões instaladas em `tw_schema_version`. As alterações de esquema atuais são idempotentes; mantenha backup do banco antes de atualizar os JARs.

## Backup e restauração

Faça backup com o servidor parado ou com uma política consistente do PostgreSQL:

```powershell
pg_dump --format=custom --file="C:\backups\tw-network.dump" --host=localhost --port=5432 --username=tw_user tw_network_db
```

Restaure em uma base vazia ou de recuperação, não sobre a base ativa sem plano de rollback:

```powershell
createdb --host=localhost --port=5432 --username=tw_user tw_network_restore
pg_restore --clean --if-exists --no-owner --host=localhost --port=5432 --username=tw_user --dbname=tw_network_restore "C:\backups\tw-network.dump"
```

Valide a base restaurada e as tabelas `tw_users`, `tw_machines`, `tw_spawners`, `tw_cheques` e `tw_schema_version` antes de direcionar o servidor para ela. Armazene credenciais fora do histórico do terminal e proteja os arquivos de backup com permissões adequadas.

## Compatibilidade

Os artefatos são compilados para Java 8 e destinados ao servidor Spigot/Paper 1.8.8. Teste os JARs em uma cópia do servidor antes de atualizar produção; APIs NMS e o protocolo visual não são portáveis para versões arbitrárias.
