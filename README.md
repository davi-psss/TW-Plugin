# TW Network

Monorepo de plugins para uma rede Minecraft de progressão Tycoon/Rankup, construído para **Purpur 26.3** e **Java 25**. O conjunto reúne economia, minas, ranks, terrenos, spawners, máquinas, farms, caixas e as funcionalidades partilhadas pelo servidor.

> **Compatibilidade estrita:** os artefatos são compilados com release Java 25 contra a API Purpur 26.3. Os descritores dos nove plugins internos exigem essa versão da API.

## Índice

- [Visão geral e módulos](#visão-geral-e-módulos)
- [Arquitetura](#arquitetura)
- [Comandos e permissões](#comandos-e-permissões)
- [Configuração](#configuração)
- [Compilação e instalação](#compilação-e-instalação)
- [Desenvolvimento](#desenvolvimento)

## Visão geral e módulos

O projeto é um **monorepo Maven**: o POM raiz agrega nove plugins JAR independentes, que partilham APIs por dependências Maven e integram-se através dos serviços, eventos e dados do `tw-core`. O Maven compila os módulos na ordem necessária; o servidor deve carregar cada dependência antes do plugin que a utiliza. O lobby é gerido pelo DeluxeHub, instalado separadamente no servidor.

| Módulo | Responsabilidade | Dependências internas principais |
|---|---|---|
| `tw-core` | API comum, dados de jogador, conexão PostgreSQL, menus e eventos internos. | — |
| `tw-economia` | Economia, tabela de preços de mineração, boosters e cheques. | `tw-core` |
| `tw-ranks` | Rankups, prestígios, tags e placeholders de progressão. | `tw-core` |
| `tw-minas` | Central de mineração, eventos, missões/cartinhas, oficina e ranking. | `tw-core`, `tw-economia` |
| `DeluxeHub` (plugin externo) | Ponto de lobby, pulo duplo e launchpads; itens fixos, scoreboard e tablist desativados para preservar o inventário do minigame. | — |
| `Multiverse-Inventories` (plugin externo) | Separação de inventários entre mundos; configuração para minas/plots fica pendente até esses mundos existirem. | `Multiverse-Core` |
| `tw-terrenos` | Agência e eventos de terrenos/plots. | `tw-core` |
| `tw-maquinas` | Máquinas colocáveis que geram saldo com combustível, upgrades e armazenamento. | `tw-core`, `tw-economia` |
| `tw-spawners` | Loja e spawners empilháveis, produção de mobs, recompensas e hologramas. | `tw-core`, `tw-economia` |
| `tw-farms` | Plantações físicas registadas, colheita, upgrades e silos virtuais. | `tw-core`, `tw-economia`, `tw-spawners` |
| `tw-caixas` | Chaves virtuais, caixas, sorteios, recompensas, efeitos e correio. | `tw-core`, `tw-economia`, `tw-minas`, `tw-spawners`, `tw-maquinas`, `tw-farms`, `tw-ranks`, `tw-terrenos` |

### Estrutura do repositório

```text
pom.xml                         POM agregador, propriedades e dependências geridas
build-and-deploy.bat            Build, testes, staging, backup e deploy local
OPERATIONS.md                   Procedimentos operacionais, backup e notas dos sistemas
tw-<modulo>/
  pom.xml                       POM do plugin e dependências do módulo
  src/main/java/                Código Java organizado por comandos, menus, managers,
                                listeners, modelos e repositories
  src/main/resources/           plugin.yml e configurações/resources do plugin
  src/test/java/                Testes unitários presentes em alguns módulos
```

## Arquitetura

### Camadas e responsabilidades

- **Menus (GUI):** apresentam estado e opções ao jogador e encaminham interações; não devem concentrar persistência ou regras de negócio.
- **Managers:** aplicam regras e coordenam o estado dos sistemas (economia/progressão, produção, compras, upgrades, recompensas).
- **Repositories:** isolam SQL e persistência. `tw-core` fornece a conexão partilhada; os módulos mantêm tabelas específicas.
- **Listeners:** traduzem eventos Bukkit (entrada/saída, colocação, quebra, colheita, morte e interação) em operações dos managers e eventos internos.
- **Modelos e eventos TW:** representam entidades persistentes e permitem que sistemas cooperem sem misturar as suas regras. Por exemplo, o Core publica eventos de mineração, colheita, ciclos de máquinas e abates de spawners.

O fluxo típico é **evento/comando → manager → repository**, com callbacks a regressar à thread do servidor antes de atualizar inventários, jogadores ou GUIs.

### Persistência e thread do servidor

O backend atualmente implementado é **PostgreSQL**, não MySQL/MariaDB: o Core usa JDBC `jdbc:postgresql`, HikariCP e tabelas versionadas por `tw_schema_version`. Os dados comuns de jogadores ficam em `tw_users`; os módulos mantêm tabelas próprias, como `tw_spawners`, `tw_machines`, `tw_farms`, `tw_crate_wallet` e `tw_cheques`.

As operações normais de leitura/escrita acionadas durante o jogo são encaminhadas de forma assíncrona pelos managers/repositories e filas de persistência, evitando fazer I/O de banco no fluxo normal da main thread. **Exceção importante na implementação atual:** a conexão/bootstrap e inicialização de esquema no arranque, algumas cargas iniciais dos managers e a gravação final síncrona no encerramento ainda podem executar I/O no thread de lifecycle. Por isso, “a main thread nunca bloqueia por banco” é a regra para operações de gameplay e para novo código, mas não uma garantia absoluta de todos os caminhos de arranque/desligamento existentes.

Não adicione chamadas JDBC bloqueantes a comandos, menus ou listeners. Para uma migração para MySQL/MariaDB seria necessário alterar e validar o driver, URL JDBC e SQL específico de PostgreSQL (incluindo `ON CONFLICT`); não basta trocar as credenciais.

### Política de drops e mecânicas virtuais

Os sistemas geridos pelo TW evitam criar entidades de item no chão quando a recompensa pode ser creditada diretamente:

- As colheitas de culturas físicas reconhecidas por `tw-farms` são encaminhadas para o armazenamento/silo da cultura; o menu permite vender, recolher e configurar Auto-Sell.
- A mineração e os ciclos geridos publicam eventos internos e podem aplicar pagamentos, tokens, bônus e progressão sem depender de drops físicos.
- Os spawners possuem modos virtuais que acumulam ganhos e recursos no próprio estado do spawner; abates elegíveis concedem as recompensas configuradas. Há também modo físico configurável, portanto não se deve generalizar a política virtual a toda entidade ou bloco vanilla do servidor.
- Quando um inventário não comporta uma recompensa de caixa, os itens excedentes podem ser guardados no correio virtual para recolha posterior.

Ao acrescentar uma mecânica, prefira atualizar inventário, saldo ou armazenamento de forma controlada em vez de fazer spawn de pilhas de itens no mundo. Respeite os limites de processamento definidos nas configurações.

### Itens especiais e compatibilidade de dados

Itens colocáveis, ferramentas melhoradas, consumíveis e recompensas transportam identificadores e estado em **Persistent Data Containers**, através do utilitário partilhado `TWItemTags`. Em alguns itens, a lore apresenta informação ao jogador; não deve ser tratada como fonte de verdade para identificadores ou níveis. As integrações de ActionBar, cabeçalho/rodapé do TAB e dados de itens usam APIs Bukkit/Paper, sem referências a internals NMS versionados.

## Comandos e permissões

Os comandos abaixo são declarados nos `plugin.yml` dos módulos. Salvo indicação contrária, a permissão é **não declarada no descritor**. Nenhum dos `plugin.yml` atuais define `permission-message`; handlers com verificação própria enviam uma mensagem personalizada, enquanto comandos protegidos apenas pelo descritor ficam sujeitos à mensagem padrão de permissão do Bukkit.

| Módulo | Comando e aliases | Permissão no descritor | `permission-message` |
|---|---|---|---|
| `tw-core` | Não regista comandos. | — | — |
| `tw-economia` | `/booster` | `tw.admin` | Não configurada |
|  | `/money` (`/saldo`, `/bal`, `/balance`) | — | Não configurada |
|  | `/pay` (`/pagar`) | — | Não configurada |
|  | `/baltop` (`/magnatas`, `/topmoney`) | — | Não configurada |
|  | `/eco` | `tw.admin` | Não configurada |
|  | `/cheque` | — | Não configurada |
| `tw-ranks` | `/rankup`, `/prestigio`, `/rankupmax` | — | Não configurada |
|  | `/ranks` (`/rank`) | — | Não configurada |
|  | `/twadmin` | `tw.admin` | Não configurada |
| `tw-minas` | `/mina` (`/minas`) | — | Não configurada |
| `DeluxeHub` (externo) | `/lobby` (`/hub`, `/spawn`) | — | Configuração em `plugins/DeluxeHub/commands.yml` |
|  | `/setlobby` | `deluxehub.command.setlobby` | Configuração em `plugins/DeluxeHub/commands.yml` |
| `tw-terrenos` | `/terreno` (`/terrenos`, `/plot`, `/p`) | — | Não configurada |
| `tw-maquinas` | `/maquinas` (`/maquina`) | — | Não configurada |
| `tw-spawners` | `/spawners` (`/spawner`, `/geradores`) | — | Não configurada |
| `tw-farms` | `/farm` (`/farms`) | — | Não configurada |
| `tw-caixas` | `/key` (`/caixas`) | — | Não configurada |
|  | `/caixa` | `tw.admin` | Não configurada |

`tw.admin` também é verificada em ações de administração dentro de alguns menus/listeners. Os comandos `/booster`, `/eco`, `/twadmin` e `/caixa` verificam essa permissão no descritor e/ou no handler. `tw-farms` e `tw-caixas` declaram `tw.admin` com `default: op`; não há `permission-message` customizada para essa permissão.

### Funções principais por módulo

- **`tw-core`:** inicializa o banco partilhado, carrega/salva perfis de jogador, fornece hierarquias de ranks e mundos configuráveis, menus comuns, scoreboard e eventos de integração. Não possui comandos.
- **`tw-economia`:** `/money` consulta saldo; `/pay` transfere para jogador; `/baltop` consulta o ranking; `/eco` administra saldos; `/booster` configura multiplicador global; `/cheque` cria cheques físicos, cuja validação/redemption é tratada por listener. Os preços de blocos são configurados por material.
- **`tw-ranks`:** `/rankup` avança um nível, `/rankupmax` compra avanços possíveis, `/prestigio` evolui após o rank `MAX`, `/ranks` abre a GUI e `/twadmin` permite configurar rank/prestígio de jogador. A sequência e custos são configurados em `tw-core/config.yml` e `tw-ranks/ranks.yml`; PlaceholderAPI é opcional.
- **`tw-minas`:** `/mina` abre a Central de Mineração, com acesso ao teleporte para a mina adequada, catálogo, oficina de picareta, cartinhas/missões e ranking. Inclui eventos de mina configuráveis e integra os blocos minerados com a economia.
- **`DeluxeHub` (plugin externo):** `/lobby` (`/hub`, `/spawn`) regressa ao spawn configurado e `/setlobby` define-o. O pulo duplo está ativo apenas no mundo `world`; scoreboard, tablist e itens personalizados de entrada estão desativados para preservar o inventário normal e evitar conflito com os plugins existentes. O Multiverse-Inventories separará os inventários quando os mundos de plot e mina forem criados e configurados.
- **`tw-terrenos`:** `/terreno` abre a agência de terrenos; o listener integra os eventos de terrenos com o estado partilhado. O alias `/p` também é registado.
- **`tw-maquinas`:** `/maquinas` abre loja/central; máquinas colocadas geram saldo por ciclo conforme tipo, stack, combustível/bateria e upgrades. O painel permite recolher ganhos e gerir melhorias/armazenamento; há hologramas e persistência.
- **`tw-spawners`:** `/spawners` abre a loja de geradores. Spawners registados podem empilhar, gerar entidades ou operar em modo virtual, acumular moeda/tokens/recursos e aplicar melhorias, boosters e Auto-Sell. Limites de produção e stack são configuráveis; hologramas exibem o estado.
- **`tw-farms`:** `/farm` e `/farm silo` (`/farm armazem`) abrem o armazém; `/farm loja` (`/farm shop`) abre a loja. As plantações físicas registadas geram itens para silos virtuais, com produtos normais/dourados, capacidade, upgrades, Auto-Sell, fertilizante e boosters. O clique com a enxada especial pode colher uma área configurada. `tw.admin` permite ações administrativas sobre farms de outros jogadores.
- **`tw-caixas`:** `/key` (`/caixas`) abre o painel de chaves. Subcomandos: `/key forja`, `/key enviar <jogador> <tipo> <quantidade>`, `/key abrir <tipo> [quantidade]`, `/key correio` e `/key efeitos`. Chaves, pity e efeitos são virtuais; as GUIs incluem painel, pré-visualização, abertura manual/em massa e forja. Recompensas seguem pesos configuráveis e podem incluir dinheiro, tokens, recursos ou itens; `/caixa` administra chaves, caixas físicas, itens-efeito e recarga da configuração.

Uso administrativo de `/caixa`:

```text
/caixa dar <jogador> <tipo> <quantidade>
/caixa setar <tipo>       (olhe para o bloco a configurar)
/caixa remover            (olhe para a caixa física)
/caixa daritem <jogador> <efeito> [cargas]
/caixa reload
```

Tipos de caixa configurados por padrão: `mineira`, `sanguinaria`, `tecnologica`, `natureza` e `divina`. A abertura em massa tem limite configurável; pity e recompensas também são parametrizados.

## Configuração

As configurações por módulo são copiadas para a pasta de dados do respetivo plugin no primeiro arranque. Valores de exemplo e tipos de recompensa estão nos `src/main/resources/config.yml`; não se deve editar os arquivos de `target/` como fonte.

### PostgreSQL do TW-Core

Configure a base partilhada usando as variáveis de ambiente recomendadas:

```text
TW_DB_HOST
TW_DB_PORT
TW_DB_DATABASE
TW_DB_USERNAME
TW_DB_PASSWORD
```

As variáveis de ambiente têm prioridade sobre `postgresql.*` em `plugins/tw-core/config.yml`. O padrão é host `localhost`, porta `5432` e base `tw_network_db`; usuário e senha precisam ser fornecidos. Não versione credenciais reais. Consulte [OPERATIONS.md](OPERATIONS.md) para procedimentos de backup/restauro e tabelas usadas.

Outras configurações relevantes:

- `tw-core/config.yml`: nomes dos mundos de mina/plots e ordem de ranks/prestígios.
- `tw-economia/config.yml`: preços por material, multiplicador inicial e máximo do booster.
- `tw-ranks/ranks.yml`: custos, prefixos, recompensas e prestígios.
- `tw-spawners/config.yml`, `tw-maquinas/config.yml` e `tw-farms/config.yml`: tipos, requisitos, produção, upgrades e limites.
- `tw-caixas/config.yml`: limites de abertura, pity, probabilidades/pesos e recompensas.

Faça backup da base antes de substituir plugins ou alterar schemas. As configurações devem ser validadas em ambiente de teste antes de produção.

## Compilação e instalação

### Pré-requisitos

- JDK 25 configurado no `JAVA_HOME` e disponível no `PATH`.
- Maven compatível com o projeto.
- Acesso aos repositórios Maven declarados no POM para obter a API Purpur 26.3 e dependências.
- Servidor Purpur 26.3 e PostgreSQL configurado para o Core.

### Build de todos os módulos

Na raiz do repositório:

```powershell
mvn clean install
```

O `install` compila os dez módulos na ordem do reactor, executa os testes configurados e instala os artefatos no repositório Maven local. Os JARs são gerados em `tw-*/target/` com o padrão `tw-<modulo>-1.0-SNAPSHOT.jar`.

Para compilar um módulo e os módulos internos de que depende, por exemplo `tw-caixas`:

```powershell
mvn -pl tw-caixas -am clean package
```

Use `-U` se precisar forçar atualização de dependências em cache. Evite `-rf :tw-caixas` ao construir isoladamente: essa retomada exclui módulos anteriores necessários do reactor.

### Deploy

1. Pare o servidor antes de substituir plugins.
2. Faça backup da base e dos JARs atualmente instalados.
3. Copie os dez arquivos `tw-*-1.0-SNAPSHOT.jar` gerados em cada `target/` para a pasta `plugins/` do servidor. Instale o conjunto compatível, não apenas módulos dependentes isolados.
4. Configure `plugins/tw-core/config.yml` ou as variáveis `TW_DB_*`.
5. Inicie o servidor e confirme no log que `tw-core` e os plugins dependentes iniciaram sem erro.

No Windows, [build-and-deploy.bat](build-and-deploy.bat) automatiza build/testes, validação dos artefatos, staging, backup e cópia dos dez JARs para `..\TW-Network\plugins`. Execute com o servidor parado; o script verifica se o servidor parece estar ativo e interrompe o deploy ou pede confirmação quando não consegue identificar o processo. Após o build, o servidor deve ser iniciado manualmente.

## Desenvolvimento

- Mantenha dependências partilhadas no POM pai e dependências de plugin declaradas como `provided` quando fornecidas pelo servidor ou por outro plugin.
- Registe comandos, aliases, permissões e dependências de carregamento em `plugin.yml`.
- Mantenha SQL nos Repositories e regras nos Managers; não faça I/O de banco síncrono a partir da main thread em caminhos de gameplay.
- Faça alterações a inventários, entidades e APIs Bukkit na main thread. Regressos de operações assíncronas devem ser agendados de volta para a thread do servidor.
- Reutilize eventos e utilitários do Core, incluindo `TWItemTags`, filas assíncronas e validação de configurações.
- Adicione ou atualize testes dos módulos afetados e valide o conjunto com `mvn clean install`.
- Preserve a compatibilidade com Java 25 e Purpur 26.3; não reintroduza APIs internas/NMS versionadas.
