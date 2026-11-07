---
description: Adiciona um evento de áudio semântico respeitando o contrato do README
argument-hint: [NOME_DO_EVENTO, ex.: ECHO_PINGED]
allowed-tools: Read, Grep, Glob, Edit, Write, Bash(make test)
---
Adicione o evento $ARGUMENTS.

Contrato (README, seção Eventos e áudio):
1. Defina antes da implementação: produtor, payload, instante, cooldown ou repetição, prioridade, posição espacial, consumidores (áudio, VFX, UI) e teste.
2. Adicione o tipo em GameEvent.Type e o cue semântico em AudioCue (UPPER_SNAKE_CASE, nome de ação ou resultado, nunca instrumento nem arquivo).
3. Publique o evento no produtor real. Um tipo declarado sem produtor e consumidor não conta como implementado.
4. Mapeie o cue para arquivo apenas em JavaFxAudioOutput. O core não conhece caminho nem formato.
5. Se precisar de som novo, use `make audio-assets` (o harness pedirá confirmação) e registre fonte, autor e licença em docs/audio/SOURCES.md.
6. Escreva o teste de publicação única por ocorrência e rode `make test`.
7. Atualize a matriz de eventos do README.
