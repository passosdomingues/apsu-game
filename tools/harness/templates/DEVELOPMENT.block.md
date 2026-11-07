## Harness do Claude Code

O repositório inclui um harness para o Claude Code, que convive com o Makefile e o AGENTS.md sem substituí-los.

- Instalação: `python3 tools/harness/install.py` (idempotente). Verificação: `python3 tools/harness/verify.py` ou `make harness-verify`.
- Alvos auxiliares (em `tools/harness/harness.mk`): `make backlog-lint`, `make sprint-status`, `make harness-verify`, `make harness-help`.
- Jogar e medir sem janela: `python3 tools/harness/play/ai_play.py` (display virtual, capturas e relatório em `target/ai-play/`).
- Backlog, sprints e Definição de Pronto em `docs/product/`.
- Detalhes de integração em `tools/harness/README.md`.
