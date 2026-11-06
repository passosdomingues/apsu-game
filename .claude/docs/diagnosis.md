# Diagnóstico: por que o jogo treme, atira mal e roda lento

Base: leitura estática do repositório no commit `5832603` (31/10/2026). O jogo não foi executado neste ambiente, então a primeira story do Sprint 7 (APSU-F00) mede o baseline real. Cada causa abaixo cita o ponto do código e a story que a corrige.

## Tremor do herói ao usar as setas

| Id | Causa | Evidência | Story |
|---|---|---|---|
| C1 | Release e press intercalados no X11 | `InputManager.registerKeyRelease` remove a tecla na hora. Com repetição automática do X11, segurar a seta gera pares KEY_RELEASED e KEY_PRESSED. Se um tick cai entre os dois, `HeroEntity.updatePhysics` vê `left=false` e aplica `vx *= 0.86` e `ax = 0`, o que zera a aceleração por um tick e depois recupera. | APSU-F01 |
| C2 | Passo fixo sem interpolação | `GameLoop.handle` acumula tempo e roda 0, 1 ou 2 `context.update` por pulso (`STEP_NANOS = 1/60 s`, `MAX_STEPS_PER_FRAME = 4`), mas `renderer.render` desenha o estado do último tick, sem `alpha`. O `now` do `AnimationTimer` varia alguns décimos de ms, então o número de passos por frame oscila e o herói avança 0 px em um frame e 2 passos no seguinte. | APSU-F02 |
| C3 | Física em unidades por frame | `HeroEntity.updatePhysics` soma `vx`, `vy` e `accel` por tick, sem `dt`. `tailPhase += ... * 0.016` e os fatores `0.58`, `0.86`, `0.88` também são por tick. Qualquer variação de passo muda o feel. | APSU-F03 |
| C4 | Câmera rígida e parallax fracionário | `Camera.update` trava em `heroX - 0.3 * viewport` a cada tick, sem suavização nem interpolação. O fundo usa fatores de parallax fracionários sobre esse x, e `drawImage` com posição fracionária reamostra a imagem a cada frame, o que cintila. | APSU-F04 |
| C5 | Colisão por teleporte | `GameContext.pushHeroOutOfObstacle` faz `hero.setX/setY` para fora do obstáculo (mais 4 px) sem zerar a velocidade que penetra. No tick seguinte o herói volta a penetrar e é empurrado de novo, vibrando contra corais e rochas. | APSU-F05 |
| C5b | Virada instantânea | `facingRight` e o yaw do modelo trocam de um tick para outro, e esquerda mais direita juntas inverte o lado a cada tick. | APSU-G08 |

## Tiro, aura e ataque

| Id | Causa | Evidência | Story |
|---|---|---|---|
| C6 | Projétil sem modelagem e atrás da cena 3D | O tiro é um `Projectile` desenhado como círculo no Canvas por `RenderEngine`. A `SubScene` 3D fica acima do Canvas, então bolhas, partículas e brilho são cobertos pelos modelos 3D. Nada do tiro existe como asset do Blender. | APSU-R02, APSU-G02 |
| C6b | Mecânica do tiro rasa | `GameContext.tryShoot` dispara enquanto SPACE estiver pressionado, com recarga fixa de 260 ms. A tecla também é a de interação (`isInteractPressed`). A bolha anda 16 px por tick, tem hitbox de 28x28 contra um visual de 16, mata qualquer inimigo em um acerto e não tem alcance, recuo, flash de boca nem trilha. O ponto de saída (`getBubbleSpawnX/Y`) é um deslocamento fixo. | APSU-G01, G03, G04 |
| C7 | Aura desligada no perfil padrão | `GameLoop` inicia com `reducedEffects = !"high".equals(apsu.quality)`, e o padrão é `balanced`, então `LightingEngine.drawBioluminescentHalo` fica inativo. Quando ativo, é desenhado no Canvas, atrás do modelo 3D. | APSU-G05 |
| C8 | Animação de nado e ataque | O nado é o mesmo OBJ com rotação em Z de menos de 1 grau (`Runtime3DLayer`). O ataque troca para outro OBJ (`hero-attack`) por 0,26 s com avanço de 8 px. Os scripts do Blender já têm `swim_cycle_keyframes` e `attack_pose_keyframes`, mas o runtime não consome frames. | APSU-G06, G07 |

## Lentidão geral

| Id | Causa | Evidência | Story |
|---|---|---|---|
| P1 | Draw calls e malhas pesadas | `ObjModelLoader` cria um `MeshView` e um `PhongMaterial` por material do OBJ. Medido com `python3 .claude/tools/mesh_budget.py`: herói 18.312 triângulos e 42 materiais, pose de ataque 19.824 e 48, boss 32.039 e 9, peixe sombrio 8.247 e 6, corrente abissal 25.728 triângulos e 15 materiais, recifes 6.336 triângulos e 82 materiais. O herói tem dois atores (`hero-swim` e `hero-attack`) na cena. Nenhum modelo de herói, inimigo, boss ou cenário cabe hoje no orçamento do CLAUDE.md. | APSU-R03, R04, R05, R06 |
| P2 | Camada 3D composta sobre Canvas em resolução lógica fixa | `SubScene` com buffer de profundidade e `Canvas` 1366x768, ambos escalados para a janela. Em pipeline de software o custo da 3D domina o frame. | APSU-F00, R07 |
| P3 | Alocação no loop de render | `Font.font` aparece 24 vezes em `RenderEngine` e 13 em `UIRenderer`, quase todas dentro de métodos chamados a cada frame. Há também `Color.rgb`, `deriveColor` e gradientes criados por frame. `python3 .claude/hooks/hotpath_lint.py` lista os pontos. | APSU-R01 |
| P4 | Degradação só visual | Abaixo de 50 FPS `GameLoop` reduz luz e partículas, mas nunca a geometria 3D, então o gargalo principal permanece. | APSU-R03 a R06 |
| P5 | Sem culling de atores 3D | `Runtime3DLayer` sincroniza todos os atores a cada frame, inclusive os fora da tela. | APSU-R06 |
| P6 | JVM sem ajuste de pipeline | O Dockerfile usa `-Dprism.order=es2,sw,j2d`. Sem verificar o pipeline ativo não se sabe se o jogo cai em software. | APSU-F00, R07 |

## Ordem de ataque

1. Medir (F00).
2. Remover o tremor na raiz: F01, F02, F04, F05, depois F03.
3. Tirar o custo de render: R01, R02, R03 a R06, R07.
4. Reconstruir tiro, aura e animações: G01 a G08.
5. Congelar com gate de performance e playtest: Q01 a Q04.
