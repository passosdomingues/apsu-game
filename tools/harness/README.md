# Harness do Claude Code para As Águas de Apsu

Camada adicional que convive com o que o repositório já tem. Nada existente é substituído.

## Como se encaixa no que já existe

| Já existe | O que o harness faz com isso |
|---|---|
| AGENTS.md (13 regras) | É importado pelo CLAUDE.md e prevalece. Os hooks aplicam as regras 01, 04, 06, 07, 08, 10, 11 e 12 de forma automática; as demais viram itens da Definição de Pronto. |
| Makefile | Continua sendo o único orquestrador. O harness chama `make test`, `make build` e `make coverage`. Uma linha `-include tools/harness/harness.mk` no fim acrescenta os alvos `backlog-lint`, `sprint-status`, `harness-verify` e `harness-help`, sem alterar o alvo padrão. |
| Docker, docker-compose | Não são usados nem alterados. |
| README, ARCHITECTURE, DEVELOPMENT | Não são reescritos. DEVELOPMENT.md recebe um bloco delimitado por marcadores (regra 09). |
| .gitignore | Recebe `.claude/state/` e `.claude/settings.local.json` (regra 07). |
| CODEOWNERS, CONTRIBUTING, .github | Intocados. |
| Configuração global do usuário em `~/.claude` | O `.claude/settings.json` do projeto se soma a ela. Listas de permissão se unem e `deny` vence. |

Nomes novos usam o prefixo `apsu-` (comandos, subagentes, skill) para não colidir com nada.

## Instalação

```
python3 tools/harness/install.py     # idempotente, pode rodar quantas vezes quiser
python3 tools/harness/verify.py      # ou: make harness-verify
```

O instalador mescla `.claude/settings.json` se ele já existir, e substitui apenas o trecho entre `<!-- apsu-harness:begin -->` e `<!-- apsu-harness:end -->` em CLAUDE.md e DEVELOPMENT.md. Texto seu fora dos marcadores não é tocado.

## O que há dentro

- `CLAUDE.md`: contexto, fluxo, pilares de design e convenções (bloco gerenciado).
- `.claude/settings.json`: permissões em três níveis (allow, ask, deny) e quatro eventos de hook.
- `tools/harness/hooks/apsu_hook.py`: hooks em um único script, biblioteca padrão, sem venv.
- `.claude/commands/`: `/apsu-aiplay`, `/apsu-audit`, `/apsu-story`, `/apsu-bug`, `/apsu-qa`, `/apsu-perf`, `/apsu-audio-event`, `/apsu-sprint-status`, `/apsu-dod`, `/apsu-backlog-add`.
- `.claude/agents/`: `apsu-reviewer`, `apsu-playtest-planner`, `apsu-perf-auditor`.
- `.claude/rules/`: regras por caminho (Java do runtime, assets, áudio e eventos, documentos de produto).
- `.claude/skills/apsu-user-story/`: formato e estimativa de user stories.
- `docs/product/`: BACKLOG.md (48 stories, 197 pontos), SPRINTS.md (10 sprints) e DEFINITION_OF_DONE.md.
- `tools/harness/backlog.py`: lint, relatório e próximas stories do backlog.

## Guardrails

Bloqueiam (o agente recebe o motivo e corrige):

- edição de `target/`, `*.class`, `*.jar`, logs e `*.blend1`;
- `git add -A`, `git add .`, add de artefatos, `git commit --no-verify`, commit fora de Conventional Commits;
- `git reset --hard`, `git clean -f`, push forçado;
- `make run`, `make all`, `make` sem alvo e `mvn javafx:run`, que abrem a janela e não retornam;
- criação de venv;
- emojis em arquivos de texto e travessão longo em Markdown;
- MPI em GameLoop, GameContext, RenderEngine e Runtime3DLayer.

Pedem confirmação do humano:

- edição de GameContext, RenderEngine e GameLoop;
- exclusão de assets, `make assets-regenerate` e `make generate-characters`;
- edição manual de `src/main/resources/`, de `.claude/settings.json`, `CLAUDE.md`, `AGENTS.md` e `tools/harness/`;
- nome de arquivo de áudio fora do adaptador e termos de dark mode.

Ao encerrar um turno, se algum `.java` foi alterado, o hook Stop roda `make test` (regra 04) e devolve a falha ao agente. Ele não entra em laço (respeita `stop_hook_active`). Para pular numa sessão: `APSU_SKIP_STOP_TEST=1`. Limite de tempo: `APSU_STOP_TIMEOUT` (padrão 900 s).

## Jogar com IA

`python3 tools/harness/play/ai_play.py --label es2 --prism es2` (ou `/apsu-aiplay`). Dependências do sistema: java, Xvfb, xdotool e ImageMagick (`sudo apt install xvfb xdotool imagemagick mesa-utils libgl1-mesa-dri`). A ferramenta sobe um display virtual, abre o jogo, executa um cenário JSON de teclas (`tools/harness/play/scenarios/`), tira capturas e grava em `target/ai-play/<rótulo>/` o `report.md` (pipeline, renderizador, avisos de SCENE3D, FPS e pulse por estado). Com `--assert-3d` sai com código 1 se o 3D estiver indisponível, útil para regressão. Para comparar os dois casos do projeto: `--prism es2` (3D por OpenGL) e `--prism sw` (o que o Windows usa quando o Direct3D falha).

## Limites conhecidos

- O agente joga por `/apsu-aiplay` em display virtual (Mesa em software). Isso cobre ver o que é desenhado e medir um piso de desempenho. Áudio, sensação de jogo e desempenho em GPU real continuam com o humano; `/apsu-qa` e `apsu-playtest-planner` geram o roteiro.
- Os hooks são Python 3 e exigem `python3` no PATH (no Windows, use Git Bash ou WSL, como o `make` do projeto já exige).
- Se o `AGENTS.md` mudar uma regra, atualize os hooks correspondentes em `apsu_hook.py` e rode `make harness-verify`.
