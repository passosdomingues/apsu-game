---
description: Revisa o impacto de frame-time e alocação de uma mudança no loop de 60 FPS (regra 11)
argument-hint: [arquivos ou story; vazio para usar o diff atual]
allowed-tools: Read, Grep, Glob, Bash(git diff:*), Bash(git status:*)
---
Revise o impacto de desempenho de: $ARGUMENTS (se vazio, use o diff atual, `git diff`).

Procure no caminho executado por quadro (GameLoop, GameContext.update, RenderEngine.render, Runtime3DLayer):
- alocações por quadro (new, boxing, streams, String.format, listas temporárias);
- trabalho repetido que poderia ser cacheado ou calculado uma vez;
- chamadas síncronas de E/S, áudio ou MPI (regra 12);
- desenho Canvas redundante ou fora da câmera.

Saída: tabela arquivo:linha, problema, custo estimado (alto, médio, baixo), correção sugerida. Termine com o comando que o humano deve rodar para medir: `APSU_PERF=1 make run` (e `APSU_PERF=1 APSU_JFR=1 make run` seguido de `make perf-summary` se houver suspeita de GC). Você pode medir um piso com `/apsu-aiplay` (Mesa em software); a medição em GPU real continua com o humano.
