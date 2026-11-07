# Definição de Pronto

Uma story só vai para `done` quando todos os itens abaixo valem. A lista aplica o AGENTS.md; não o substitui. Em caso de conflito, o AGENTS.md prevalece (regra 13: parar e pedir esclarecimento).

## Por story

1. Todos os critérios de aceitação da story estão marcados `[x]` no BACKLOG.md, cada um com verificação reproduzível (teste, medição ou playtest descrito).
2. Houve auditoria prévia do código afetado (regra 02), por `/apsu-audit` ou equivalente.
3. A mudança toca somente arquivos da story (regra 01) e não reformata linhas alheias (regra 05).
4. A arquitetura foi preservada; lógica nova foi para classes novas e não para GameContext ou RenderEngine (regras 03 e 10).
5. `make test` passa (regra 04). Se a story é de runtime, o resultado de `make coverage` não regrediu nos pacotes tocados.
6. Código no loop principal foi avaliado quanto a alocação e frame-time (regra 11). Stories com critério de desempenho têm a medição anotada: por `/apsu-aiplay` (piso em software) e por `APSU_PERF=1 make run` no hardware de referência, feita por um humano.
7. Nenhuma chamada MPI foi adicionada ao loop de frame (regra 12).
8. Nenhum artefato de build foi commitado e nenhum asset foi excluído sem justificativa explícita (regras 07 e 08).
9. README, ARCHITECTURE e DEVELOPMENT foram atualizados quando interfaces, utilitários ou fluxos mudaram (regra 09). O README só marca algo como IMPLEMENTADO depois de confirmado no runtime.
10. Eventos novos têm produtor, payload, frequência, consumidor e teste definidos; cues de áudio são semânticos, sem nome de arquivo no core.
11. Interfaces seguem o tema claro, sem dark mode, e nenhum texto novo contém emojis ou travessões longos.
12. O commit segue Conventional Commits e é atômico (regra 06), referenciando o ID da story no corpo, por exemplo `Refs: US-010`.
13. `python3 tools/harness/backlog.py lint` passa.

## Por sprint

1. Todas as stories planejadas estão `done` ou foram movidas, com motivo, para a sprint seguinte.
2. A fase afetada foi jogada do início ao fim em revisão coletiva e a checklist de QA do README foi preenchida com evidência.
3. `Sprint ativa:` em SPRINTS.md foi atualizada para a próxima sprint.
4. Bugs encontrados na revisão viraram stories ou bugs com prioridade e dono.
