# Como o harness funciona

## Hooks

Todos em Python com biblioteca padrão. Entram em `settings.json`. Código de saída 2 bloqueia e devolve a mensagem ao Claude.

| Hook | Evento | O que faz |
|---|---|---|
| `session_start.py` | SessionStart | Injeta branch, story ativa, último perf gate e o lembrete de prioridade. Depois de compactação, reinjeta o resumo salvo |
| `prompt_context.py` | UserPromptSubmit | Se o pedido fala de tremor, lentidão, tiro, aura ou modelos, injeta a regra e as causas relevantes |
| `guard_bash.py` | PreToolUse Bash | Bloqueia force push, reset hard, clean, `git add -A` e `git add .`, add de `target`, `.class`, `.jar`, `.log`, remoção de assets, sudo, curl em shell e commit fora do Conventional Commits |
| `guard_edit.py` | PreToolUse Edit, Write | Bloqueia binários, `target/`, `.git/`, áudio versionado, emojis, travessão longo em texto, marcadores de pendência em `src/` e `.claude/` e crescimento líquido acima de 40 linhas em `GameContext` e `RenderEngine` |
| `hotpath_lint.py` | PostToolUse Edit, Write | Nas linhas alteradas de métodos `update*`, `render*`, `draw*`, `paint*`, `physics*`, acusa `new`, `String.format`, `Font.font`, `Color.web/rgb`, `deriveColor`, stream, lambda, print, carga de recurso. Marcador `// hot-alloc-ok` libera uma linha |
| `pre_compact.py` | PreCompact | Salva branch, story e diff em `state/precompact.md` |
| `stop_gate.py` | Stop | Se houve mudança em Java ou `pom.xml` e o estado ainda não passou nos testes, roda `mvn -o -q test` e bloqueia o fim se falhar. Evita laço com `stop_hook_active` |
| `statusline.py` | statusLine | Mostra branch, story ativa e último perf gate |

## Permissões

- `allow`: leitura, edição em `src/`, `tools/`, `.claude/` e `*.md`, testes, build, git de leitura, commit e add por caminho.
- `ask`: comandos `make` que regeneram assets, `package`, `docker-run`, push, merge, rebase, `blender`, e edição de `pom.xml`, `Makefile`, `Dockerfile`, CI, `.blend` e `src/main/resources`.
- `deny`: segredos, `target/`, `.git/`, áudio, force push, reset hard, clean, `git add -A`, `rm -rf` em raízes, sudo, curl e wget.

Para ajustes pessoais use `.claude/settings.local.json` (ignorado pelo git).

## Ferramentas

| Ferramenta | Uso |
|---|---|
| `tools/perf_gate.py` | Lê as linhas `[PERF]` do `GameLoop` e aprova ou reprova contra o orçamento. `--save` grava o resumo para a barra de status |
| `tools/mesh_budget.py` | Triângulos e materiais de cada OBJ contra o orçamento por tipo. `--only` filtra |
| `tools/sprint_report.py` | Valida o formato das stories, a escala Fibonacci e as somas, e imprime o resumo. `--story ID` mostra uma story |
| `tools/validate_harness.py` | Integridade do próprio harness |
| `tests/test_hooks.py` | 15 testes dos hooks e do perf_gate |

## Comandos

| Comando | Para que serve |
|---|---|
| `/story-start ID` | Branch, story ativa e plano |
| `/story-done ID` | Verifica a Definição de Concluído e marca tarefas |
| `/sprint-status` | Resumo de pontos e stories |
| `/new-story` | Cria story no formato validado |
| `/perf-baseline` | Roteiro e análise de baseline |
| `/perf-gate LOG` | Veredito contra o orçamento |
| `/hunt-jitter` | Investigação de tremor com subagente |
| `/diagnose-input` | Confirma o par release/press do X11 |
| `/audit-hotpath` | Lista alocações em métodos quentes |
| `/mesh-budget` | Orçamento de malhas |
| `/blender-export` | Plano de export de um modelo |
| `/feel-review` | Revisão de game feel do diff |
| `/commit` | Commit atômico em Conventional Commits |

## Subagentes

`perf-profiler`, `jitter-hunter`, `render-architect`, `blender-technical-artist`, `gameplay-feel-reviewer`, `qa-playtester`. Todos somente leitura, exceto o artista técnico, que edita scripts de Blender.

## Skills

`game-feel-fluidity`, `javafx-perf-budget`, `blender-runtime-bake`.

## Fluxo diário

1. `/sprint-status`
2. `/story-start APSU-F01`
3. Implementar com testes. O lint de hot path e os guardas rodam sozinhos.
4. `/story-done APSU-F01`
5. `/commit`
