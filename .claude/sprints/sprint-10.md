# Sprint 10: Ataque, polimento, regressão e release (27/11 a 04/12)

Capacidade: 32 pontos. Planejado: 31.

Meta: demo de 04/12 com 60 FPS estáveis, ataque com antecipação e peso, e gate de performance na CI.

### APSU-G04 | Flash de boca, recuo e hit-stop | 5 pts | Marcos
Como jogador, quero impacto no tiro e no acerto, para o combate ter peso.

Complexidade 5: efeitos curtos e sincronizados, sem alterar regras.

Critérios de aceite:
- AC1: Ao disparar, o flash de boca de G02 toca por 6 frames na ponta do tridente.
- AC2: Recuo de 6 px no herói por 80 ms, com retorno suave, sem alterar a hitbox.
- AC3: Ao acertar um inimigo, hit-stop de 50 ms (dois a três ticks) só do inimigo e do projétil, sem pausar o herói.
- AC4: Shake de 2 px por 90 ms ao acertar, desligável pelo menu de efeitos.
- AC5: Nenhum desses efeitos aloca por frame (lint limpo) e o custo somado é menor que 0,5 ms.

Tarefas:
- [ ] G04.1 Flash de boca no Canvas de efeitos (1,5 h)
- [ ] G04.2 Recuo suave no herói (1,5 h)
- [ ] G04.3 Hit-stop local por entidade (2 h)
- [ ] G04.4 Opção de shake e medição (1 h)

### APSU-G07 | Animação de ataque | 5 pts | Cawe
Como jogador, quero um ataque com antecipação, disparo e recuperação, para ler a ação.

Complexidade 5: reaproveita o pipeline de frames da G06.

Critérios de aceite:
- AC1: 6 frames de ataque exportados por `attack_pose_keyframes`: antecipação (2), disparo (2) e recuperação (2), mesma topologia do nado.
- AC2: O runtime sobrepõe a troca de frames do ataque à do nado por 0,26 s, sem trocar de ator.
- AC3: O projétil nasce no frame de disparo (4º frame), sincronizado com o flash.
- AC4: O ataque pode ser interrompido por um novo disparo do buffer sem salto visual.
- AC5: Dado ataque contínuo por 30 s, então nenhum frame acima de 33 ms causado pela animação.

Tarefas:
- [ ] G07.1 Keyframes e exportação dos 6 frames (3 h)
- [ ] G07.2 Sobreposição no `HeroAnimator` (2 h)
- [ ] G07.3 Sincronismo com o disparo do `WeaponController` (1 h)

### APSU-G08 | Virada e inclinação suaves | 3 pts | Rafa
Como jogador, quero o herói virando com suavidade, para a troca de lado não ser um estalo.

Complexidade 3: interpolação de yaw e pitch em `Runtime3DLayer`.

Critérios de aceite:
- AC1: O yaw interpola entre 0 e 180 graus em 120 ms com curva suave.
- AC2: O pitch acompanha `vy` até 8 graus, com amortecimento.
- AC3: Esquerda e direita juntas mantêm o último lado (sem alternar).
- AC4: Teste de `HeroPose` cobre os três casos.

Tarefas:
- [ ] G08.1 `HeroPose` com yaw e pitch amortecidos (1,5 h)
- [ ] G08.2 Aplicar em `Runtime3DLayer.place` (1 h)
- [ ] G08.3 Teste (1 h)

### APSU-Q01 | Playtest de fluidez P1 a P5 | 5 pts | Erik
Como produto, quero provar a fluidez nas cinco fases, para fechar a demo com confiança.

Complexidade 5: execução longa e registro cuidadoso.

Critérios de aceite:
- AC1: Cada fase jogada por 5 min no hardware de referência com `APSU_PERF=1`, e `perf_gate` aprovado.
- AC2: Planilha de bugs com severidade, fase, passos e commit para cada problema de fluidez encontrado.
- AC3: Nenhum bug de severidade alta em aberto (tremor, travada acima de 100 ms, tiro sem resposta).
- AC4: Registro em `.claude/perf/playtest-final.md` com os números por fase.

Tarefas:
- [ ] Q01.1 Preparar roteiro por fase (1 h)
- [ ] Q01.2 Jogar e registrar P1 a P5 (4 h)
- [ ] Q01.3 Consolidar o relatório (1 h)

### APSU-Q02 | Gate de performance e testes na CI | 5 pts | Marcos
Como equipe, quero a CI barrando regressões de física e desempenho, para não perder o ganho.

Complexidade 5: acrescenta jobs ao workflow existente.

Critérios de aceite:
- AC1: O job de testes da CI roda também `python3 -m unittest discover -s .claude/tests`.
- AC2: Um job headless executa uma simulação determinística de 3.600 ticks do herói e da câmera, falha se a trajetória divergir do arquivo dourado.
- AC3: O job valida orçamento de malha com `python3 .claude/tools/mesh_budget.py --only hero --only enemy --only boss --only scenery`.
- AC4: O PR template ganha o campo "saída do perf_gate" para mudanças de render ou movimento.
- AC5: A CI continua abaixo de 8 minutos.

Tarefas:
- [ ] Q02.1 Teste de trajetória determinística (2 h)
- [ ] Q02.2 Job no `.github/workflows/ci.yml` (1,5 h)
- [ ] Q02.3 Atualizar o template de PR (0,5 h)
- [ ] Q02.4 Medir tempo da CI (0,5 h)

### APSU-Q03 | Documentação e release | 3 pts | Rafa
Como equipe, quero a documentação coerente com o jogo, para a demo e a avaliação.

Complexidade 3: texto e conferência.

Critérios de aceite:
- AC1: ARCHITECTURE descreve o loop com interpolação, a pilha de três camadas e a animação por frames de malha.
- AC2: DEVELOPMENT documenta `APSU_INPUT_TRACE`, o gate de performance e as flags escolhidas em R07.
- AC3: README atualizado com o orçamento de performance cumprido e o resumo do baseline contra o final.
- AC4: Tag de release criada depois do `make test` verde e do `perf_gate` aprovado.

Tarefas:
- [ ] Q03.1 Atualizar ARCHITECTURE (1,5 h)
- [ ] Q03.2 Atualizar DEVELOPMENT e README (1,5 h)
- [ ] Q03.3 Tag e pacote (0,5 h)

### APSU-Q04 | Regressão de gameplay | 5 pts | Erik
Como jogador, quero que nada tenha quebrado nas regras, para a demo ser confiável.

Complexidade 5: cobre colisão, dano, dificuldade, boss e persistência.

Critérios de aceite:
- AC1: Roteiro cobre dano por contato, invulnerabilidade, geyser, lava, correntes, zonas de pressão, guardiões, boss e vitória.
- AC2: Resultados iguais ao baseline em cada item, ou diferença aprovada pelo Rafa e registrada em `.claude/docs/decisions.md`.
- AC3: As três dificuldades jogadas até o fim da P1 sem anomalia.
- AC4: O progresso persistido (`persistProgress`) sobrevive a reiniciar o jogo.

Tarefas:
- [ ] Q04.1 Roteiro de regressão (1,5 h)
- [ ] Q04.2 Execução nas três dificuldades (3 h)
- [ ] Q04.3 Relatório e decisões (1 h)
