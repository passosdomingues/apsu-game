# As Águas de Apsu: guia do agente

Jogo 2.5D em Java 21 e JavaFX 21 (Maven, JUnit 5, FXGL 21.1 no classpath). Trabalho de Computação Gráfica da UNIFAL-MG (DCE533). Código em `src/main/java/br/apsu`, assets 3D em `personagens/` (fontes Blender) e `src/main/resources` (runtime).

## Prioridade absoluta

Jogabilidade fluida. Um commit que piora o frame pacing, a latência de entrada ou a estabilidade visual do herói é reprovado, mesmo que adicione conteúdo. Leia `.claude/docs/diagnosis.md` antes de mexer em movimento, câmera, render ou tiro.

## Comandos de projeto

```
make test            # JUnit 5 (obrigatório antes de concluir)
make build           # compila
make run             # executa o jogo
APSU_PERF=1 make run # log de FPS, p50/p95, update/render, GC por segundo
APSU_AUDIO=0 APSU_PERF=1 make run   # isola render do áudio
make perf-summary    # resume o JFR (com APSU_JFR=1)
make -C .claude check   # valida este harness
```

## Orçamento de performance (vale como critério de aceite)

Referência: notebook com GPU integrada, 4 threads, 8 GB, resolução 1366x768 em tela cheia.

| Métrica | Meta | Falha |
|---|---|---|
| FPS médio por fase | 60 | abaixo de 55 |
| p95 do intervalo de frame | até 18 ms | acima de 22 ms |
| Frames acima de 33 ms por minuto | 0 | mais de 3 |
| update (média) | até 2 ms | acima de 4 ms |
| render (média) | até 9 ms | acima de 12 ms |
| GC por minuto em jogo | até 2 pausas | mais de 6 |
| Latência tecla a movimento | 1 tick ao apertar, até 2 ao soltar | mais de 2 ticks ao apertar |
| Pausa por carga de modelo no thread FX | 0 ms | qualquer valor |

Ferramenta: `python3 .claude/tools/perf_gate.py <log>` lê o log `[PERF]` e devolve aprovado ou reprovado contra esta tabela.

## Regras do repositório (AGENTS.md, resumidas)

1. Escopo restrito: só os arquivos da tarefa.
2. Auditar antes de editar.
3. Preservar a arquitetura, salvo refactor pedido.
4. `make test` verde antes de concluir.
5. Sem reformatar linhas alheias.
6. Commits atômicos em Conventional Commits (`perf:`, `fix:`, `feat:`, `test:`, `docs:`).
7. Nunca versionar `target/`, `*.class`, `*.jar`, logs.
8. Nunca apagar assets sem justificativa técnica escrita.
9. Atualizar README, ARCHITECTURE, DEVELOPMENT quando interfaces ou workflows mudarem.
10. Mudanças modulares: não engordar `GameContext` (1296 linhas) nem `RenderEngine` (643). Extraia classes novas.
11. Código do loop de 60 Hz não aloca: nada de `new`, `String.format`, `Font.font`, `Color.web`, `deriveColor`, gradientes, lambdas ou boxing dentro de `update` e `render`.
12. Nenhuma chamada MPI no loop.
13. Conflito entre instrução e AGENTS.md: parar e perguntar.

## Regras de fluidez (inegociáveis)

- Movimento e física em unidades por segundo, com `dt` fixo de 1/60 s. Nada de constantes por frame sem `dt`.
- O render interpola entre o estado anterior e o atual (`alpha` do acumulador). Nunca desenhar posição crua do tick.
- A câmera segue o herói com suavização crítica e arredonda para pixel inteiro só no desenho final.
- Parallax e sprites 2D: posição de desenho arredondada para inteiro. Imagens de fundo pré-escaladas para altura 768 na carga.
- Entrada: soltar e repetir tecla no X11 gera par release/press. Todo release é confirmado só depois de uma janela curta sem press (ver `InputManager` na story APSU-F01).
- Colisão com obstáculo nunca teleporta sem zerar a componente de velocidade que penetra.
- Camada 3D (SubScene) fica acima do Canvas: tudo que precisa ficar atrás de personagens vai no Canvas, tudo que precisa ficar na frente (projétil, aura, VFX de impacto) segue a decisão da story APSU-R02.
- Modelo runtime (checar com `/mesh-budget`): herói até 6.000 triângulos e 6 materiais; inimigo comum 4.000 e 3; boss 12.000 e 6; NPC 6.000 e 4; elemento de cenário 2.500 e 2.

## Fluxo de trabalho com sprints

Backlog em `.claude/sprints/`. Antes de codar: `/story-start <ID>`. Ao terminar: `/story-done <ID>` (roda a Definição de Pronto). Estado ativo fica em `.claude/state/active-story`. Pontuação em Fibonacci (1, 2, 3, 5, 8, 13). Nenhuma story acima de 13: quebrar.

Sprints de performance planejadas em `.claude/sprints/perf-sprints.md` (APSU-R01 a R07 e correlatos). Sprints de gameplay em `.claude/sprints/sprint-07.md` a `sprint-10.md`.

## Estilo de entrega

- Texto de documentação enxuto, sem emojis, sem travessão longo.
- Entregas sem pendências, placeholders ou itens para revisão: resolver a ambiguidade e registrar a decisão no commit ou em `.claude/docs/decisions.md`.
- Interfaces e ferramentas visuais com tema claro apenas.
- Scripts Python auxiliares: biblioteca padrão, sem venv; rodar por Makefile ou Docker (`make -C .claude help`).

## Mapa do código

| Área | Arquivos |
|---|---|
| Loop e tempo | `core/GameLoop`, `core/FrameMetrics` |
| Estado e regras | `core/GameContext` (God object), `core/InputManager`, `core/Camera` |
| Herói e física | `model/hero/HeroEntity`, `HeroType` |
| Tiro | `GameContext.tryShoot`, `model/environment/Projectile`, `RenderEngine.drawProjectiles` |
| Render 2D | `graphics/RenderEngine`, `LightingEngine`, `UIRenderer`, `SpriteManager` |
| Render 3D | `graphics/Runtime3DLayer`, `ObjModelLoader` |
| Assets | `personagens/scripts/*.py` (bpy), `tools/assets/*.py`, `src/main/resources/{models,sprites,backgrounds}` |

## Subagentes e comandos

Subagentes em `.claude/agents/`: `perf-profiler`, `jitter-hunter`, `render-architect`, `blender-technical-artist`, `gameplay-feel-reviewer`, `qa-playtester`. Comandos em `.claude/commands/` (digite `/` para listar). Hooks em `.claude/hooks/` (descritos em `.claude/docs/harness.md`).

<!-- apsu-harness:begin -->
# As Águas de Apsu: instruções para o Claude Code

Jogo 2.5D em Java 21 e JavaFX 21 (Maven), projeto da disciplina de Computação Gráfica da UNIFAL-MG. O README é o documento central de produção; ARCHITECTURE.md descreve o runtime.

## Precedência e convivência com o harness existente

@AGENTS.md

- O AGENTS.md (13 regras) é a autoridade. Este arquivo só acrescenta. Em conflito, pare e pergunte (regra 13).
- O Makefile do projeto é o único orquestrador. Este harness não cria Makefile nem Docker novos; os alvos auxiliares estão em `tools/harness/harness.mk`, incluído no fim do Makefile.
- Comandos, subagentes, regras e skill do harness usam o prefixo `apsu-` para não colidir com nada existente.

## Comandos do projeto que você pode usar

- `make test`, `make build`, `make coverage`, `make system-info`, `make help`.
- `make backlog-lint`, `make sprint-status`, `make harness-verify`.
- Alvos de assets (`make assets`, `make export-model MODEL=...`, `make audio-assets`) pedem confirmação do humano.
- Nunca rode `make run`, `make all`, `make` sem alvo nem `mvn javafx:run`: abrem a janela e não retornam.
- Para jogar, use `/apsu-aiplay` (ou `python3 tools/harness/play/ai_play.py`): roda o jogo em display virtual, envia teclas, tira capturas e mede o frame-time. Requer java, Xvfb, xdotool e ImageMagick. O Mesa em software mede o pior caso; áudio e a sensação de jogo continuam para o humano.

## Fluxo de trabalho

1. `/apsu-sprint-status` para ver a sprint ativa e a próxima story.
2. `/apsu-audit ID` antes de editar (regra 02).
3. `/apsu-story ID` para implementar com testes; `/apsu-bug` para bugs; `/apsu-audio-event` para eventos de áudio.
4. `/apsu-aiplay` para ver o jogo rodando; `/apsu-perf` para qualquer código no loop de 60 FPS; `/apsu-dod` antes de propor commit.
5. O subagente `apsu-reviewer` revisa o diff contra as regras antes do commit.

## Backlog e produto

- Backlog: @docs/product/BACKLOG.md. Sprints: @docs/product/SPRINTS.md. Definição de Pronto: @docs/product/DEFINITION_OF_DONE.md.
- Pontos em Fibonacci, critérios em "Dado, quando, então". Valide com `python3 tools/harness/backlog.py lint`.

## Pilares de design (vindos do feedback de playtest)

- O jogador sempre sabe o que fazer e para onde ir: objetivo no HUD, seta, aviso de interação, cutscenes de foco.
- A primeira fase ensina jogando. Nenhum bloqueio de progressão sem explicação visível.
- O radar é uma habilidade temporária (ecolocalização, tecla Q), não um mapa permanente.
- Perigo é legível antes do contato (telegraph), mas o chefe causa dano ao encostar.
- Todo obstáculo com colisão tem visual correspondente.

## Convenções

- Idioma: português do Brasil em documentos, mensagens de commit e texto de jogo.
- Escrita modesta, sem emojis e sem travessões longos, em qualquer entrega.
- Interfaces (menu, HUD, diálogos, GPS) somente em tema claro, sem dark mode. A escuridão das fases profundas é cenário e não muda isso.
- Scripts Python usam só a biblioteca padrão; sem venv.
- Commits atômicos em Conventional Commits, com `Refs: US-xxx`. Não commite sem pedido explícito.
- Não entregue itens pendentes ou placeholders: resolva ambiguidades e registre a decisão no próprio documento. O README usa "A DEFINIR" apenas para decisões de produto que dependem da equipe; não o use para esconder trabalho seu.
- Não marque como IMPLEMENTADO algo que existe só como arquivo; confirme o uso no runtime.

## Guardrails ativos (hooks em .claude/settings.json)

O harness bloqueia: edição de artefatos de build, `git add` amplo, commit fora de Conventional Commits, `git reset --hard`, push forçado, `make run`, emojis e travessões longos em arquivos novos, MPI no loop. Pede confirmação para: GameContext, RenderEngine, GameLoop, exclusão de assets, regeneração de .blend, recursos empacotados e arquivos do próprio harness. Ao encerrar, roda `make test` se algum .java foi alterado (regra 04).
<!-- apsu-harness:end -->
