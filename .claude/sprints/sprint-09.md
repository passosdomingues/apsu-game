# Sprint 9: Tiro, aura e animação de nado (20/11 a 26/11)

Capacidade: 32 pontos. Planejado: 31.

Meta: atirar é satisfatório e legível, a aura aparece em todos os perfis e o herói nada com animação de verdade.

### APSU-G01 | Tiro responsivo | 5 pts | Rafa
Como jogador, quero atirar com a resposta imediata de um tiro bem feito, para sentir controle.

Complexidade 5: separa tiro de interação e adiciona buffer.

Critérios de aceite:
- AC1: O tiro usa a tecla J e também SPACE fora de diálogo; em diálogo SPACE só avança texto.
- AC2: Buffer de tiro de 120 ms: um press durante a recarga dispara assim que a recarga termina.
- AC3: Recarga de 180 ms (antes 260 ms), constante em `HeroEntity`, alterável por `HeroType`.
- AC4: Latência do press ao projétil criado é no máximo 1 tick, medida em teste com `InputManager` e relógio simulado.
- AC5: Segurar a tecla mantém cadência constante, sem rajada dupla.
- AC6: Teste JUnit `ShootingTest` cobre AC1 a AC5.

Tarefas:
- [ ] G01.1 `WeaponController` novo com recarga e buffer (2 h)
- [ ] G01.2 Mapear a tecla J e a regra de diálogo no `InputManager` (1 h)
- [ ] G01.3 Substituir `tryShoot` por chamada ao controlador (1 h)
- [ ] G01.4 Testes (2 h)

### APSU-G02 | Projétil modelado no Blender | 8 pts | Cawe
Como jogador, quero um projétil com visual próprio, para o tiro ter identidade.

Complexidade 8: modelagem, shader translúcido, render de sprite sheet e integração.

Critérios de aceite:
- AC1: Script `personagens/scripts/13_projetil_bolha.py` gera a bolha de energia (esfera de baixo poliedro com shader translúcido e rim light conforme `_apsu_shared_lib.py`).
- AC2: Sprite sheet de 8 frames de oscilação de 64x64 px, mais 6 frames de impacto de 96x96 px e 6 de flash de boca de 96x96 px, gerados por `make render-sprites`.
- AC3: Os PNG entram em `src/main/resources/sprites/projectiles/` com fundo transparente e sem halo escuro nas bordas.
- AC4: `SpriteManager` carrega as folhas na inicialização, não durante o jogo.
- AC5: Aprovação visual do Rafa sobre os fundos das fases 1 a 5.

Tarefas:
- [ ] G02.1 Modelar e materializar a bolha no Blender (3 h)
- [ ] G02.2 Keyframes de oscilação, impacto e flash (3 h)
- [ ] G02.3 Renderizar e empacotar as folhas (2 h)
- [ ] G02.4 Carga em `SpriteManager` e teste de existência dos arquivos (1 h)

### APSU-G03 | Projétil com movimento e acerto corretos | 5 pts | Rafa
Como jogador, quero que o acerto combine com o que vejo, para confiar no tiro.

Complexidade 5: altera colisão e criação de projéteis.

Critérios de aceite:
- AC1: A hitbox do projétil é um círculo de raio igual a 90% do raio visual.
- AC2: O ponto de saída é calculado a partir da posição interpolada do herói e do lado para o qual ele olha, na boca do tridente.
- AC3: Alcance máximo de 900 px e vida útil de 1,2 s; ao fim, o projétil estoura com o frame de impacto.
- AC4: O projétil herda 25% da velocidade horizontal do herói.
- AC5: O projétil é desenhado no Canvas de efeitos com a folha de G02, com a posição interpolada.
- AC6: Testes cobrem saída, alcance e hitbox.

Tarefas:
- [ ] G03.1 `Projectile` com raio, vida e herança de velocidade (2 h)
- [ ] G03.2 Ponto de saída pela posição do tridente (1,5 h)
- [ ] G03.3 Desenho por sprite sheet em `EffectsRenderer` (2 h)
- [ ] G03.4 Testes (1,5 h)

### APSU-G05 | Aura visível e bonita | 5 pts | Marcos
Como jogador, quero a aura do herói em todos os perfis, para ele se destacar no escuro.

Complexidade 5: textura radial, pulso e integração com perfis.

Critérios de aceite:
- AC1: A aura é uma `Image` radial gerada uma vez na carga (gradiente suave de 5 paradas) por cor de `HeroType`.
- AC2: O desenho usa só `drawImage` com escala e alfa; nenhuma alocação por frame.
- AC3: Pulso lento (período de 2,4 s) de 6% na escala e 12% no alfa.
- AC4: A aura fica ativa nos perfis `balanced` e `low`; só some se o FPS médio cair abaixo de 40 por 3 s.
- AC5: Fica no Canvas de fundo, atrás do modelo 3D, com raio de 1,6 vezes a altura do herói.
- AC6: O custo medido da aura é menor que 0,3 ms por frame.

Tarefas:
- [ ] G05.1 Gerador de textura radial (`AuraSprites`) (1,5 h)
- [ ] G05.2 Desenho e pulso no Canvas de fundo (1 h)
- [ ] G05.3 Ajustar `LightingEngine.reducedEffects` para não desligar a aura (1 h)
- [ ] G05.4 Medir custo (0,5 h)

### APSU-G06 | Animação de nado por frames de malha | 8 pts | Cawe
Como jogador, quero ver o herói nadar com ondulação real, para o movimento ter vida.

Complexidade 8: pipeline novo de animação em Blender e troca de malha no runtime.

Critérios de aceite:
- AC1: O Blender exporta 8 frames de nado (`swim_cycle_keyframes`) com a mesma topologia e UV; o script valida contagem de vértices igual entre frames.
- AC2: O runtime carrega os 8 `TriangleMesh` na inicialização e troca o mesh de cada `MeshView` com `setMesh` conforme a fase do ciclo.
- AC3: A velocidade do ciclo é proporcional à velocidade do herói (4 a 12 ciclos por segundo, mesma faixa de `tailPhase`).
- AC4: Parado, o herói exibe ciclo lento de respiração (0,7 ciclo por segundo).
- AC5: Memória adicional de malhas menor que 8 MiB, medida pelo `perf_gate` (heap).
- AC6: Sem queda de FPS maior que 0,3 ms de render em relação ao herói estático.

Tarefas:
- [ ] G06.1 Keyframes de nado e exportação por frame (3 h)
- [ ] G06.2 Verificador de topologia em `tools/assets` (1,5 h)
- [ ] G06.3 `HeroAnimator` no runtime com troca de mesh (3 h)
- [ ] G06.4 Ajuste de velocidade e respiração (1,5 h)
