# Operação dos plugins TW

## Build e deploy

Execute `build-and-deploy.bat` com o servidor parado. O script compila e testa o reactor com atualização forçada do cache Maven, copia os dez JARs que acabou de gerar para uma pasta temporária, faz backup dos JARs atuais em `TW-Network/backups` e instala os artefatos. O staging copia diretamente os artefatos locais, sem uma segunda execução Maven isolada que tente resolver os módulos internos em repositórios remotos. Se a cópia falhar, tenta restaurar o backup.

Para compilar apenas `tw-caixas` com os módulos internos de que depende, execute a partir da raiz deste repositório `mvn -pl tw-caixas -am clean package`. Se uma falha anterior deixou uma resolução negativa em cache, repita com `mvn -U -pl tw-caixas -am clean package` para forçar a atualização. Não use `-rf :tw-caixas` para esse caso: `-rf` retoma a partir de `tw-caixas` e exclui `tw-farms` e os outros módulos anteriores do reactor, fazendo o Maven procurá-los nos repositórios remotos.

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

Valide a base restaurada e as tabelas `tw_users`, `tw_machines`, `tw_spawners`, `tw_farms`, `tw_farm_crops`, `tw_farm_storage`, `tw_cheques`, `tw_crate_wallet`, `tw_crate_pity`, `tw_crate_opening`, `tw_crate_mail`, `tw_crate_player_effects`, `tw_physical_crates` e `tw_schema_version` antes de direcionar o servidor para ela. Armazene credenciais fora do histórico do terminal e proteja os arquivos de backup com permissões adequadas.

## Farms

`tw-farms` depende de `tw-core`, `tw-economia` e `tw-spawners` e usa o PostgreSQL compartilhado. A produção vem de plantações físicas construídas no terreno: as culturas crescem com as regras normais do Minecraft, e os drops de produtos associados a uma cultura registada são encaminhados ao armazém `/farm` em vez de permanecerem no chão. O detalhe de cada cultura no armazém permite vender, recolher, ligar/desligar Auto-Sell e devolver ao silo os itens identificados que foram recolhidos. Os boosters aplicam-se às plantações do mesmo tipo; a duração não acumula, e usar um booster mais forte melhora a potência sem reiniciar o temporizador ativo. Cada ativação nova é limitada por `farm.growth-booster-max-duration-seconds` (predefinição de 60 minutos). `/farm loja` vende os itens de plantio e boosters e o botão do armazém volta a `/farm`; `/farm` e `/farm silo` abrem o armazém. As farms-bloco antigas não produzem mais, mas continuam acessíveis para venda/recolha do stock e desmontagem. A associação das culturas é ao jogador que as plantou nos mundos definidos como plots em `tw-core`.

## Caixas

`tw-caixas` mantém as chaves e o pity como dados virtuais no PostgreSQL; eles não são itens de inventário. `/key` abre o painel, `/key enviar <jogador> <tipo> <quantidade>` transfere chaves, `/key forja` abre a forja e `/key correio` recolhe itens excedentes que não couberam no inventário. Cada chave sorteia uma recompensa ponderada; a abertura manual mostra uma animação sem escolha de prémio, e cada chave em abertura em massa também rende exatamente uma recompensa. O pity na abertura em massa avança à metade da velocidade. O Selo da Fortuna Divina no inventário garante um prémio lendário na próxima abertura, manual ou em massa. Os limites operacionais e as tabelas de recompensas são configuráveis em `plugins/tw-caixas/config.yml`. Administradores usam `/caixa dar`, `/caixa setar`, `/caixa remover`, `/caixa daritem` e `/caixa reload`.

Os consumíveis de mineração são ativados com clique direito no ar ou num bloco: o item é consumido e as cargas ficam associadas ao UUID do jogador em `tw_crate_player_effects`, portanto funcionam após a troca do lobby/spawn para o mundo de minas e não dependem do inventário daquela dimensão. O Contrato duplica o pagamento por bloco; a Carga Sísmica amplia a área de uma picaretada por carga; a Broca aplica +20% ao pagamento por bloco. O Íman de Almas também é carregado na conta e concede uma alma extra por carga consumida em abates elegíveis. Usa `/key efeitos` para consultar os saldos. As alterações são guardadas em lote a cada 40 ticks, ao sair e no encerramento normal do plugin.

Os restantes consumíveis são aplicados diretamente no alvo indicado pela lore: cápsula e talismã em spawners registados, bateria/lubrificante/overclock em máquinas registadas, booster/foice/semente em plantações registadas e orbe no Rank MAX. O Fragmento da Natureza é um item colecionável empilhável, não um consumível sem ação associada. `divine_seal` é aplicado automaticamente na próxima abertura. `/caixa daritem` só aceita efeitos com utilização implementada e cria os itens nativos de farm com as tags necessárias.

## Compatibilidade

Os artefatos são compilados com Java 25 e destinados ao Purpur 26.3. Teste os JARs em uma cópia desse servidor antes de atualizar produção; os `plugin.yml` exigem API 26.3.
