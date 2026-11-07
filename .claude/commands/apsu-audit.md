---
description: Auditoria prévia somente leitura de uma área do código (regra 02 do AGENTS.md)
argument-hint: [classe, pacote, fase ou story, ex.: US-001]
allowed-tools: Read, Grep, Glob, Bash(git status:*), Bash(git log:*), Bash(python3 tools/harness/backlog.py:*)
---
Faça uma auditoria somente leitura de: $ARGUMENTS

Se for um ID de story, leia-a antes com `python3 tools/harness/backlog.py show ID`.

Passos:
1. Leia AGENTS.md, ARCHITECTURE.md e a seção da fase no README. Localize as classes envolvidas com Grep (GameContext, RenderEngine, Runtime3DLayer, EventBus, AudioCue, Camera, InputManager, QuadTree, conforme o caso).
2. Mapeie: quem produz, quem consome, onde está o estado, quais testes existem em src/test.
3. Confirme ou refute cada hipótese das notas técnicas da story com evidência (arquivo e trecho).
4. Aponte o ponto de extensão que evita editar GameContext, RenderEngine e GameLoop (regras 10 e 11).
5. Liste riscos de frame-time e de regressão.

Saída: resumo de no máximo 25 linhas com (a) o que existe, (b) o que falta, (c) arquivos que a mudança deve tocar, (d) arquivos que não deve tocar. Não edite nada.
