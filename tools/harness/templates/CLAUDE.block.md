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
