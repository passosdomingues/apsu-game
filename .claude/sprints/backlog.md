# Backlog de fluidez e desempenho

Objetivo do produto: jogabilidade fluida a 60 FPS estáveis, herói sem tremor, tiro e ataque com peso, aura visível, tudo dentro do orçamento do `CLAUDE.md`.

Calendário: continua o roadmap existente em `docs/development/apsu_sprints_backlog.xlsx` (sprints de sexta a quinta, demo em 04/12/2026).

| Sprint | Período | Meta | Pontos |
|---|---|---|---|
| S7 | 06/11 a 12/11 | Medir e eliminar o tremor | 29 |
| S8 | 13/11 a 19/11 | Render sem gargalo | 34 |
| S9 | 20/11 a 26/11 | Tiro, aura e animação de nado | 31 |
| S10 | 27/11 a 04/12 | Ataque, polimento, regressão e release | 31 |

Total: 125 pontos.

## Escala de complexidade

Fibonacci: 1, 2, 3, 5, 8, 13. Pontua esforço, incerteza e risco juntos.

| Pontos | Significado |
|---|---|
| 1 | Alteração trivial em um arquivo, sem risco |
| 2 | Pequena, bem conhecida, um módulo |
| 3 | Moderada, dois módulos ou um teste novo |
| 5 | Maior, vários arquivos, risco médio |
| 8 | Grande, incerteza real ou mudança transversal |
| 13 | Muito grande, deve ser quebrada antes de entrar na sprint |

Nenhuma story passa de 8 neste backlog.

## Responsáveis

Nomes e funções vêm do roadmap existente.

| Pessoa | Função neste backlog |
|---|---|
| Rafa | Core, mecânica, movimento, tiro, aceite |
| Marcos | Render, VFX, integração, otimização, plataforma |
| Erik | QA, testes de jogabilidade |
| Cawe | Modelagem 3D do herói e do projétil, animação |
| Wagner | Malhas de inimigos e boss |
| Carlos Eduardo | Malhas de cenário |

Rafa tem capacidade reduzida em 11/11 (registrado no roadmap). As stories dele no S7 foram escolhidas para caber nisso: F03 e F05 têm margem e podem terminar em 12/11.

## Definição de Pronto (DoR)

- Story com critérios de aceite mensuráveis.
- Dependências listadas e já concluídas.
- Estimativa em Fibonacci acordada no planning.

## Definição de Concluído (DoD)

1. Todos os critérios de aceite da story verificados e anotados no PR.
2. `make test` verde, com testes novos para regra de jogo alterada.
3. `python3 .claude/hooks/hotpath_lint.py` sem violações nos arquivos tocados.
4. Para stories de movimento, render ou assets: `APSU_PERF=1 make run` por 60 s depois do aquecimento, com `python3 .claude/tools/perf_gate.py` aprovado.
5. Commit atômico em Conventional Commits, sem `target/`, `.class`, `.jar` ou log.
6. README, ARCHITECTURE ou DEVELOPMENT atualizados se interface ou fluxo mudou.
7. Revisão por outro membro. Stories de gameplay exigem aceite do Rafa jogando.

## Dependências

```
F00 -> todas as stories de medição
F01, F02 -> F03 -> G01
F02 -> F04
R01 -> R02 -> G02, G03, G05
R03 -> G06 -> G07
G02 -> G03 -> G04
R03, R04, R05, R06 -> Q01
G01..G08 -> Q04
```

## Riscos

| Risco | Efeito | Resposta |
|---|---|---|
| Pipeline cai em software no hardware de referência | Nenhuma otimização de malha basta | F00 detecta e R07 corrige flags e driver antes de R03 |
| Troca de frames de malha exige topologia idêntica | Animação quebra | Script Blender valida contagem de vértices e UV iguais entre frames (R03, G06) |
| Interpolação introduz atraso visual de 1 tick | Sensação de resposta menor | Aceite mede latência de entrada, máximo 2 ticks na parada e 1 tick na partida |
| Migração da física muda o feel calibrado | Regressão de jogabilidade | Teste dourado contra a implementação atual com dt = 1/60 |

## Arquivos

- `sprint-07.md` a `sprint-10.md`: stories com critérios de aceite e tarefas.
- Validação de formato e soma de pontos: `python3 .claude/tools/sprint_report.py`.
