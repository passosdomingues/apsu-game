---
description: Joga o jogo em display virtual, tira capturas e resume pipeline gráfico e frame-time
argument-hint: [pipeline: es2, sw ou auto] [cenário, padrão smoke_p1]
allowed-tools: Read, Bash(python3 tools/harness/play/ai_play.py:*), Bash(make package), Bash(make build), Bash(ls:*)
---
Jogue o jogo com a ferramenta do harness: $ARGUMENTS

1. Garanta que existe build: `ls target/*jar-with-dependencies.jar`. Se não existir, rode `make package`.
2. Rode `python3 tools/harness/play/ai_play.py --label <rótulo> --prism <pipeline>`. Sem argumento, rode duas vezes: `--prism es2` e `--prism sw --assert-3d`, para comparar.
3. Abra com a ferramenta de imagem as capturas em `target/ai-play/<rótulo>/` e descreva o que está desenhado: herói, guardião, inimigos, HUD, corais, avisos. Compare com o que o critério de aceitação espera.
4. Leia `report.md` e `summary.json`: pipeline, renderizador, avisos de SCENE3D, erros `[3D]`, FPS e pulse por estado.
5. Responda com: o que foi verificado visualmente, números de desempenho, e o que ainda exige um humano (áudio, sensação de jogo, GPU real).

Limites: o display virtual usa Mesa em software, então o desempenho medido é pior que o de uma GPU dedicada e serve como piso. Nunca use `make run`.
