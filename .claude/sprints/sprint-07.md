# Sprint 7: Medir e eliminar o tremor (06/11 a 12/11)

Capacidade: 30 pontos. Planejado: 29.

Meta: o herói anda sem tremer em qualquer taxa de pulso, e o baseline de performance está registrado.

### APSU-F00 | Baseline e instrumentação | 3 pts | Marcos
Como desenvolvedor, quero um baseline medido e reproduzível, para provar cada ganho com número.

Complexidade 3: sem lógica nova, mas exige rodar o jogo em cada fase e registrar o ambiente.

Critérios de aceite:
- AC1: `make system-info` e `-Dprism.verbose=true` registrados em `.claude/perf/baseline-ambiente.txt`, informando o pipeline ativo (es2 ou sw).
- AC2: Um log de 60 s depois do aquecimento para cada fase P1 a P5 em `.claude/perf/baseline-P1.log` a `baseline-P5.log`, gerado com `APSU_AUDIO=0 APSU_PERF=1 make run`.
- AC3: `python3 .claude/tools/perf_gate.py` executado em cada log e a saída salva em `.claude/perf/baseline-resumo.txt`.
- AC4: O log de entrada (flag `APSU_INPUT_TRACE=1`) mostra a contagem de pares release/press ao segurar a seta direita por 3 s.

Tarefas:
- [ ] F00.1 Rodar `make system-info` e anotar renderer OpenGL (0,5 h)
- [ ] F00.2 Adicionar log de trace de entrada ligado por `-Dapsu.inputTrace=true` no `Makefile` como `APSU_INPUT_TRACE` (1 h)
- [ ] F00.3 Capturar os 5 logs de fase e o resumo (2 h)
- [ ] F00.4 Registrar o hardware usado e comparar com a referência do CLAUDE.md (0,5 h)

### APSU-F01 | Entrada sem tremor no X11 | 3 pts | Rafa
Como jogador, quero que segurar a seta mantenha o nado constante, para o herói não tremer.

Complexidade 3: classe pequena, mas exige teste com relógio injetado.

Critérios de aceite:
- AC1: Dado a seta direita segurada com repetição simulada (release seguido de press a cada 30 ms), quando o herói se move, então `isPressed` retorna verdadeiro em todos os ticks.
- AC2: Dado um release real (sem novo press em 25 ms), a tecla deixa de estar pressionada em até 2 ticks.
- AC3: `clearKeys` limpa também os releases pendentes.
- AC4: Esquerda e direita juntas resultam em direção neutra, não em alternância por tick.
- AC5: Teste JUnit `InputManagerTest` cobre AC1 a AC4 sem JavaFX em execução.

Tarefas:
- [x] F01.1 `InputManager` recebe `long nowNanos` por `tick(long)` chamado pelo `GameLoop` antes de cada update (1 h)
- [x] F01.2 Mapa de releases pendentes com `RELEASE_GRACE_NANOS = 25_000_000` (1 h)
- [x] F01.3 Resolver eixo horizontal e vertical em `axisX()` e `axisY()` com neutro para teclas opostas (1 h)
- [x] F01.4 Testes `InputManagerTest` (1,5 h)

Notas: `registerKeyPress` cancela o release pendente. O `GameContext` passa a ler `axisX()` e `axisY()` em vez de quatro `isPressed`.

### APSU-F02 | Interpolação de render | 5 pts | Rafa
Como jogador, quero movimento suave em qualquer taxa de atualização do monitor, para não ver saltos.

Complexidade 5: toca herói, inimigos, projéteis, câmera e as duas camadas de render.

Critérios de aceite:
- AC1: `GameLoop` calcula `alpha = accumulatedNanos / STEP_NANOS` e o entrega ao renderer.
- AC2: Herói, inimigos, projéteis e câmera guardam o estado anterior e são desenhados em `lerp(prev, atual, alpha)`.
- AC3: Dado pulso alternando 16,2 ms e 17,1 ms, quando o herói anda com velocidade constante, então o desvio de posição entre frames consecutivos é no máximo 1 px do ideal (teste unitário com relógio simulado).
- AC4: `perf_gate` não acusa `sim_spread` acima de 0,35 em P1 por 60 s.
- AC5: Teletransportes (troca de fase, respawn) não interpolam: `snapPrevious()` iguala anterior e atual.

Tarefas:
- [x] F02.1 Campos `prevX`, `prevY` e `snapPrevious()` em `HeroEntity`, `EnemyEntity` e `Projectile` (1,5 h)
- [x] F02.2 `Camera` guarda `prevX` e expõe `getInterpolatedX(alpha)` (1 h)
- [x] F02.3 `GameLoop` passa `alpha`; `GameRenderer.render` recebe o parâmetro (1 h)
- [x] F02.4 `Runtime3DLayer.sync` e `RenderEngine` usam posições interpoladas (2 h)
- [x] F02.5 Teste com relógio simulado (1,5 h)

### APSU-F03 | Física do herói em unidades por segundo | 8 pts | Rafa
Como desenvolvedor, quero a física independente do passo, para poder ajustar a taxa sem mudar o feel.

Complexidade 8: reescreve o núcleo do movimento e precisa preservar a calibração atual.

Critérios de aceite:
- AC1: `HeroType` expõe velocidade em px/s e aceleração em px/s2 (valores atuais multiplicados por 60 e 3600).
- AC2: Com `dt = 1/60`, a trajetória após 300 ticks com a sequência de teclas do teste dourado é igual à implementação atual com tolerância de 1e-6 px.
- AC3: Com `dt = 1/120` (dois subpassos), a posição final após 5 s difere no máximo 2 px da de `dt = 1/60`.
- AC4: Fatores por tick (0,58, 0,86, 0,62, 0,88) viram `pow(fator, dt * 60)`; `tailPhase` usa `dt`.
- AC5: Os cinco `updateP1` a `updateP5` chamam um único método `stepHero(dt)` em vez de repetir quatro `isPressed`.
- AC6: Todos os `HeroType` mantêm velocidade máxima, tempo para atingir 90% da máxima e distância de parada iguais ao atual, medidos em teste.

Tarefas:
- [ ] F03.1 Gerar o teste dourado a partir da implementação atual antes de alterar (1,5 h)
- [ ] F03.2 Converter `HeroType` e `HeroEntity.updatePhysics(dt)` (3 h)
- [ ] F03.3 Extrair `stepHero` do `GameContext` para uma classe nova `HeroController` (regra 10) (2 h)
- [ ] F03.4 Testes AC3 e AC6 (2 h)

### APSU-F04 | Câmera suave e fundo sem cintilar | 5 pts | Marcos
Como jogador, quero a câmera e o fundo estáveis, para a tela não tremer ao nadar.

Complexidade 5: câmera, parallax e pré-processamento de imagens.

Critérios de aceite:
- AC1: `Camera` segue o alvo com amortecimento crítico (`x += (alvo - x) * (1 - exp(-k * dt))`, `k = 10`) e mantém o clamp do mundo.
- AC2: A posição de desenho da câmera é arredondada para inteiro no último passo, e cada camada de parallax usa `Math.round(camX * fator)`.
- AC3: As imagens de fundo são carregadas já na altura de 768 px (sem reamostragem por frame) e desenhadas sem escala dentro do loop.
- AC4: Dado herói em velocidade máxima constante por 5 s, o desvio padrão da posição do herói na tela é menor que 0,5 px.
- AC5: O shake da câmera continua funcionando e não é arredondado para pixels fracionários que cintilem.

Tarefas:
- [x] F04.1 Amortecimento e interpolação em `Camera` (1,5 h)
- [x] F04.2 Arredondamento nas camadas de `RenderEngine.drawBackground` (1 h)
- [x] F04.3 Pré-escala das imagens na carga em `SpriteManager` (1,5 h)
- [x] F04.4 Teste de estabilidade da câmera (1 h)

### APSU-F05 | Colisão sem vibração | 5 pts | Rafa
Como jogador, quero encostar em corais e rochas sem o herói vibrar, para controlar o nado perto dos obstáculos.

Complexidade 5: afeta fases 2 a 4 e dano por contato.

Critérios de aceite:
- AC1: Em `pushHeroOutOfObstacle`, a componente de velocidade que entra no obstáculo é zerada e a posição é corrigida só pelo menor eixo de penetração, sem folga fixa de 4 px.
- AC2: Dado o herói pressionando a seta contra um coral por 3 s, então a variação de posição por tick é no máximo 0,1 px depois do primeiro contato.
- AC3: O herói desliza ao longo da parede quando a outra seta é pressionada.
- AC4: Dano por contato continua aplicado uma única vez por janela de invulnerabilidade.
- AC5: Teste JUnit com um obstáculo e entrada constante cobre AC2 e AC3.

Tarefas:
- [ ] F05.1 Resolver penetração por eixo e zerar velocidade (2 h)
- [ ] F05.2 Aplicar nos casos MOVING_OBSTACLE, VOLCANIC_ROCK e coral (1 h)
- [ ] F05.3 Testes AC2 e AC3 (2 h)
